package com.catechism.platform.service

import com.catechism.platform.repository.AppUserRepository
import com.catechism.platform.domain.explanation.*
import com.catechism.platform.repository.explanation.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class ModerationService(
    private val explanationRepository: ExplanationSubmissionRepository,
    private val reviewRepository: ExplanationReviewRepository,
    private val userRepository: AppUserRepository,
    private val explanationService: ExplanationService
) {

    /**
     * Submit a review for an explanation
     */
    fun reviewExplanation(
        explanationId: UUID,
        reviewerId: UUID,
        reviewStatus: ReviewStatus,
        qualityRating: Int?,
        accuracyScore: Int?,
        clarityScore: Int?,
        theologicalSoundnessScore: Int?,
        comments: String?
    ): ExplanationReview {
        // Validate explanation exists
        val explanation = explanationRepository.findById(explanationId).orElseThrow {
            IllegalArgumentException("Explanation not found: $explanationId")
        }

        // Validate reviewer exists
        val reviewer = userRepository.findById(reviewerId).orElseThrow {
            IllegalArgumentException("Reviewer not found: $reviewerId")
        }

        // Validate reviewer has permission (PRIEST, THEOLOGY_REVIEWER, or ADMIN)
        require(reviewer.role.name in listOf("PRIEST", "THEOLOGY_REVIEWER", "ADMIN")) {
            "User does not have permission to review explanations"
        }

        // Check if reviewer already reviewed this explanation
        val existingReview = reviewRepository.findByExplanationIdAndReviewerId(explanationId, reviewerId)
        if (existingReview != null) {
            throw IllegalArgumentException("Reviewer has already reviewed this explanation")
        }

        // Create review
        val review = ExplanationReview(
            explanation = explanation,
            reviewer = reviewer,
            reviewStatus = reviewStatus,
            reviewComments = comments,
            qualityRating = qualityRating,
            accuracyScore = accuracyScore,
            clarityScore = clarityScore,
            theologicalSoundnessScore = theologicalSoundnessScore
        )

        val savedReview = reviewRepository.save(review)

        // Update explanation status based on review
        when (reviewStatus) {
            ReviewStatus.APPROVED -> {
                explanationService.updateStatus(explanationId, ExplanationStatus.APPROVED)
            }
            ReviewStatus.REJECTED -> {
                explanationService.updateStatus(explanationId, ExplanationStatus.REJECTED)
            }
            ReviewStatus.NEEDS_REVISION -> {
                // Keep in UNDER_REVIEW or change to PENDING
                if (explanation.submissionStatus != ExplanationStatus.UNDER_REVIEW) {
                    explanationService.updateStatus(explanationId, ExplanationStatus.UNDER_REVIEW)
                }
            }
        }

        // Recalculate quality score
        explanationService.updateQualityScore(explanationId)

        return savedReview
    }

    /**
     * Get reviews for an explanation
     */
    fun getReviewsForExplanation(explanationId: UUID): List<ExplanationReview> {
        return reviewRepository.findByExplanationId(explanationId)
    }

    /**
     * Get reviews by a specific reviewer
     */
    fun getReviewsByReviewer(reviewerId: UUID): List<ExplanationReview> {
        return reviewRepository.findByReviewerId(reviewerId)
            .sortedByDescending { it.reviewedAt }
    }

    /**
     * Get average scores for an explanation
     */
    fun getAverageScores(explanationId: UUID): ExplanationScores {
        val reviews = reviewRepository.findByExplanationId(explanationId)

        if (reviews.isEmpty()) {
            return ExplanationScores(
                reviewCount = 0,
                avgQuality = null,
                avgAccuracy = null,
                avgClarity = null,
                avgTheological = null
            )
        }

        return ExplanationScores(
            reviewCount = reviews.size,
            avgQuality = reviews.mapNotNull { it.qualityRating }.average().takeIf { !it.isNaN() },
            avgAccuracy = reviews.mapNotNull { it.accuracyScore }.average().takeIf { !it.isNaN() },
            avgClarity = reviews.mapNotNull { it.clarityScore }.average().takeIf { !it.isNaN() },
            avgTheological = reviews.mapNotNull { it.theologicalSoundnessScore }.average().takeIf { !it.isNaN() }
        )
    }

    /**
     * Check if explanation needs more reviews
     */
    fun needsMoreReviews(explanationId: UUID, minimumReviews: Int = 2): Boolean {
        val reviews = reviewRepository.findByExplanationId(explanationId)
        return reviews.size < minimumReviews
    }

    /**
     * Get review consensus
     */
    fun getReviewConsensus(explanationId: UUID): ReviewConsensus {
        val reviews = reviewRepository.findByExplanationId(explanationId)

        if (reviews.isEmpty()) {
            return ReviewConsensus(
                hasConsensus = false,
                consensusStatus = null,
                approvalCount = 0,
                rejectionCount = 0,
                revisionCount = 0
            )
        }

        val approvalCount = reviews.count { it.reviewStatus == ReviewStatus.APPROVED }
        val rejectionCount = reviews.count { it.reviewStatus == ReviewStatus.REJECTED }
        val revisionCount = reviews.count { it.reviewStatus == ReviewStatus.NEEDS_REVISION }

        val total = reviews.size
        val majorityThreshold = total / 2.0

        val consensusStatus = when {
            approvalCount > majorityThreshold -> ReviewStatus.APPROVED
            rejectionCount > majorityThreshold -> ReviewStatus.REJECTED
            else -> null
        }

        return ReviewConsensus(
            hasConsensus = consensusStatus != null,
            consensusStatus = consensusStatus,
            approvalCount = approvalCount,
            rejectionCount = rejectionCount,
            revisionCount = revisionCount
        )
    }
}

/**
 * Average scores for an explanation
 */
data class ExplanationScores(
    val reviewCount: Int,
    val avgQuality: Double?,
    val avgAccuracy: Double?,
    val avgClarity: Double?,
    val avgTheological: Double?
)

/**
 * Review consensus data
 */
data class ReviewConsensus(
    val hasConsensus: Boolean,
    val consensusStatus: ReviewStatus?,
    val approvalCount: Int,
    val rejectionCount: Int,
    val revisionCount: Int
)