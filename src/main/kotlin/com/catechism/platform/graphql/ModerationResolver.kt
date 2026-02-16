package com.catechism.platform.graphql

import com.catechism.platform.domain.explanation.*
import com.catechism.platform.repository.AppUserRepository
import com.catechism.platform.service.*
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import java.util.*

@Controller
class ModerationResolver(
    private val moderationService: ModerationService,
    private val userRepository: AppUserRepository
) {

    @QueryMapping
    fun reviewsForExplanation(@Argument explanationId: UUID): List<ExplanationReviewDTO> {
        return moderationService.getReviewsForExplanation(explanationId)
            .map { it.toDTO() }
    }

    @QueryMapping
    fun explanationScores(@Argument explanationId: UUID): ExplanationScoresDTO {
        val scores = moderationService.getAverageScores(explanationId)
        return ExplanationScoresDTO(
            reviewCount = scores.reviewCount,
            avgQuality = scores.avgQuality,
            avgAccuracy = scores.avgAccuracy,
            avgClarity = scores.avgClarity,
            avgTheological = scores.avgTheological
        )
    }

    @QueryMapping
    fun reviewConsensus(@Argument explanationId: UUID): ReviewConsensusDTO {
        val consensus = moderationService.getReviewConsensus(explanationId)
        return ReviewConsensusDTO(
            hasConsensus = consensus.hasConsensus,
            consensusStatus = consensus.consensusStatus,
            approvalCount = consensus.approvalCount,
            rejectionCount = consensus.rejectionCount,
            revisionCount = consensus.revisionCount
        )
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('PRIEST', 'THEOLOGY_REVIEWER', 'ADMIN')")
    fun reviewExplanation(@Argument input: ReviewExplanationInput): ExplanationReviewDTO {
        val reviewerId = getCurrentUserId()

        val review = moderationService.reviewExplanation(
            explanationId = input.explanationId,
            reviewerId = reviewerId,
            reviewStatus = input.reviewStatus,
            qualityRating = input.qualityRating,
            accuracyScore = input.accuracyScore,
            clarityScore = input.clarityScore,
            theologicalSoundnessScore = input.theologicalSoundnessScore,
            comments = input.reviewComments
        )

        return review.toDTO()
    }

    private fun getCurrentUserId(): UUID {
        val authentication = SecurityContextHolder.getContext().authentication
        val email = authentication.principal as? String
            ?: throw IllegalStateException("User not authenticated")

        return userRepository.findByEmail(email)?.id
            ?: throw IllegalStateException("User not found")
    }

    private fun ExplanationReview.toDTO() = ExplanationReviewDTO(
        id = this.id,
        explanationId = this.explanation.id,
        reviewerId = this.reviewer.id,
        reviewerName = this.reviewer.name,
        reviewStatus = this.reviewStatus,
        reviewComments = this.reviewComments,
        qualityRating = this.qualityRating,
        accuracyScore = this.accuracyScore,
        clarityScore = this.clarityScore,
        theologicalSoundnessScore = this.theologicalSoundnessScore,
        reviewedAt = this.reviewedAt
    )
}

// DTOs
data class ExplanationReviewDTO(
    val id: UUID,
    val explanationId: UUID,
    val reviewerId: UUID,
    val reviewerName: String,
    val reviewStatus: ReviewStatus,
    val reviewComments: String?,
    val qualityRating: Int?,
    val accuracyScore: Int?,
    val clarityScore: Int?,
    val theologicalSoundnessScore: Int?,
    val reviewedAt: java.time.Instant
)

data class ReviewExplanationInput(
    val explanationId: UUID,
    val reviewStatus: ReviewStatus,
    val reviewComments: String?,
    val qualityRating: Int?,
    val accuracyScore: Int?,
    val clarityScore: Int?,
    val theologicalSoundnessScore: Int?
)

data class ExplanationScoresDTO(
    val reviewCount: Int,
    val avgQuality: Double?,
    val avgAccuracy: Double?,
    val avgClarity: Double?,
    val avgTheological: Double?
)

data class ReviewConsensusDTO(
    val hasConsensus: Boolean,
    val consensusStatus: ReviewStatus?,
    val approvalCount: Int,
    val rejectionCount: Int,
    val revisionCount: Int
)