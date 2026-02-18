package com.catechism.platform.graphql

import com.catechism.platform.domain.explanation.ExplanationVote
import com.catechism.platform.repository.AppUserRepository
import com.catechism.platform.service.VoteService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import java.util.*

@Controller
class VoteResolver(
    private val voteService: VoteService,
    private val userRepository: AppUserRepository
) {

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun myVote(@Argument explanationId: UUID): ExplanationVoteDTO? {
        val userId = getCurrentUserId()
        val vote = voteService.getUserVote(explanationId, userId) ?: return null
        return vote.toDTO()
    }

    @QueryMapping
    fun voteStatistics(@Argument explanationId: UUID): VoteStatisticsDTO {
        val stats = voteService.getVoteStatistics(explanationId)
        return VoteStatisticsDTO(
            totalVotes = stats.totalVotes,
            helpfulVotes = stats.helpfulVotes,
            unhelpfulVotes = stats.unhelpfulVotes,
            helpfulPercentage = stats.helpfulPercentage
        )
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    fun voteOnExplanation(@Argument input: VoteOnExplanationInput): ExplanationVoteDTO {
        val userId = getCurrentUserId()

        val vote = voteService.voteOnExplanation(
            explanationId = input.explanationId,
            userId = userId,
            isHelpful = input.isHelpful,
            comment = input.voteComment
        )

        return vote.toDTO()
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    fun updateVote(@Argument input: UpdateVoteInput): ExplanationVoteDTO {
        val userId = getCurrentUserId()

        val vote = voteService.updateVote(
            explanationId = input.explanationId,
            userId = userId,
            isHelpful = input.isHelpful,
            comment = input.voteComment
        )

        return vote.toDTO()
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    fun removeVote(@Argument explanationId: UUID): Boolean {
        val userId = getCurrentUserId()
        return voteService.removeVote(explanationId, userId)
    }

    private fun getCurrentUserId(): UUID {
        val authentication = SecurityContextHolder.getContext().authentication
        val email = authentication.principal as? String
            ?: throw IllegalStateException("User not authenticated")

        return userRepository.findByEmail(email)?.id
            ?: throw IllegalStateException("User not found")
    }

    private fun ExplanationVote.toDTO() = ExplanationVoteDTO(
        id = this.id,
        explanationId = this.explanation.id,
        userId = this.user.id,
        userName = this.user.name,
        isHelpful = this.isHelpful,
        voteComment = this.voteComment,
        createdAt = this.createdAt
    )
}

// DTOs
data class ExplanationVoteDTO(
    val id: UUID,
    val explanationId: UUID,
    val userId: UUID,
    val userName: String,
    val isHelpful: Boolean,
    val voteComment: String?,
    val createdAt: java.time.Instant
)

data class VoteOnExplanationInput(
    val explanationId: UUID,
    val isHelpful: Boolean,
    val voteComment: String?
)

data class UpdateVoteInput(
    val explanationId: UUID,
    val isHelpful: Boolean,
    val voteComment: String?
)