package com.catechism.platform.service

import com.catechism.platform.domain.explanation.*
import com.catechism.platform.repository.AppUserRepository
import com.catechism.platform.repository.explanation.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class VoteService(
    private val voteRepository: ExplanationVoteRepository,
    private val explanationRepository: ExplanationSubmissionRepository,
    private val userRepository: AppUserRepository,
    private val explanationService: ExplanationService
) {

    /**
     * Vote on an explanation
     */
    fun voteOnExplanation(
        explanationId: UUID,
        userId: UUID,
        isHelpful: Boolean,
        comment: String? = null
    ): ExplanationVote {
        // Validate explanation exists and is approved
        val explanation = explanationRepository.findById(explanationId).orElseThrow {
            IllegalArgumentException("Explanation not found: $explanationId")
        }

        require(explanation.submissionStatus == ExplanationStatus.APPROVED) {
            "Can only vote on approved explanations"
        }

        // Validate user exists
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found: $userId")
        }

        // Check if user already voted
        val existingVote = voteRepository.findByExplanationIdAndUserId(explanationId, userId)
        if (existingVote != null) {
            throw IllegalArgumentException("User has already voted on this explanation")
        }

        // Create vote
        val vote = ExplanationVote(
            explanation = explanation,
            user = user,
            isHelpful = isHelpful,
            voteComment = comment
        )

        val savedVote = voteRepository.save(vote)

        // Update helpful count on explanation
        if (isHelpful) {
            explanation.helpfulCount++
            explanationRepository.save(explanation)
        }

        // Recalculate quality score
        explanationService.updateQualityScore(explanationId)

        return savedVote
    }

    /**
     * Update a vote
     */
    fun updateVote(
        explanationId: UUID,
        userId: UUID,
        isHelpful: Boolean,
        comment: String? = null
    ): ExplanationVote {
        // Find existing vote
        val vote = voteRepository.findByExplanationIdAndUserId(explanationId, userId)
            ?: throw IllegalArgumentException("Vote not found")

        // If vote changed, update helpful count
        if (vote.isHelpful != isHelpful) {
            val explanation = vote.explanation
            if (isHelpful) {
                explanation.helpfulCount++
            } else {
                explanation.helpfulCount--
            }
            explanationRepository.save(explanation)
        }

        // Delete old vote and create new one (since vote fields are immutable)
        voteRepository.delete(vote)

        val newVote = ExplanationVote(
            explanation = vote.explanation,
            user = vote.user,
            isHelpful = isHelpful,
            voteComment = comment
        )

        val savedVote = voteRepository.save(newVote)

        // Recalculate quality score
        explanationService.updateQualityScore(explanationId)

        return savedVote
    }

    /**
     * Remove a vote
     */
    fun removeVote(explanationId: UUID, userId: UUID): Boolean {
        val vote = voteRepository.findByExplanationIdAndUserId(explanationId, userId)
            ?: return false

        // Update helpful count if vote was helpful
        if (vote.isHelpful) {
            val explanation = vote.explanation
            explanation.helpfulCount--
            explanationRepository.save(explanation)
        }

        voteRepository.delete(vote)

        // Recalculate quality score
        explanationService.updateQualityScore(explanationId)

        return true
    }

    /**
     * Get vote by user and explanation
     */
    fun getUserVote(explanationId: UUID, userId: UUID): ExplanationVote? {
        return voteRepository.findByExplanationIdAndUserId(explanationId, userId)
    }

    /**
     * Get all votes for an explanation
     */
    fun getVotesForExplanation(explanationId: UUID): List<ExplanationVote> {
        return voteRepository.findByExplanationId(explanationId)
            .sortedByDescending { it.createdAt }
    }

    /**
     * Get all votes by a user
     */
    fun getVotesByUser(userId: UUID): List<ExplanationVote> {
        return voteRepository.findByUserId(userId)
            .sortedByDescending { it.createdAt }
    }

    /**
     * Get vote statistics for an explanation
     */
    fun getVoteStatistics(explanationId: UUID): VoteStatistics {
        val helpfulCount = voteRepository.countHelpfulVotes(explanationId)
        val unhelpfulCount = voteRepository.countUnhelpfulVotes(explanationId)
        val totalCount = helpfulCount + unhelpfulCount

        val helpfulRatio = if (totalCount > 0) {
            (helpfulCount.toDouble() / totalCount) * 100
        } else {
            0.0
        }

        return VoteStatistics(
            totalVotes = totalCount.toInt(),
            helpfulVotes = helpfulCount.toInt(),
            unhelpfulVotes = unhelpfulCount.toInt(),
            helpfulPercentage = helpfulRatio
        )
    }

    /**
     * Check if user has voted on an explanation
     */
    fun hasUserVoted(explanationId: UUID, userId: UUID): Boolean {
        return voteRepository.findByExplanationIdAndUserId(explanationId, userId) != null
    }

    /**
     * Get top voted explanations for a question
     */
    fun getTopVotedExplanations(questionId: UUID, limit: Int = 5): List<ExplanationSubmission> {
        val explanations = explanationRepository.findByQuestionIdAndSubmissionStatus(
            questionId,
            ExplanationStatus.APPROVED
        )

        return explanations
            .sortedWith(
                compareByDescending<ExplanationSubmission> { it.qualityScore ?: 0 }
                    .thenByDescending { it.helpfulCount }
            )
            .take(limit)
    }
}

/**
 * Vote statistics
 */
data class VoteStatistics(
    val totalVotes: Int,
    val helpfulVotes: Int,
    val unhelpfulVotes: Int,
    val helpfulPercentage: Double
)