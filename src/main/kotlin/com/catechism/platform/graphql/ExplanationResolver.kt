package com.catechism.platform.graphql

import com.catechism.platform.domain.explanation.*
import com.catechism.platform.repository.AppUserRepository
import com.catechism.platform.service.*
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import java.util.*

@Controller
class ExplanationResolver(
    private val explanationService: ExplanationService,
    private val moderationService: ModerationService,
    private val flagService: FlagService,
    private val voteService: VoteService,
    private val userRepository: AppUserRepository
) {

    // =====================================================
    // Queries
    // =====================================================

    @QueryMapping
    fun explanation(@Argument id: UUID): ExplanationDTO? {
        val explanation = explanationService.getExplanationById(id) ?: return null
        return explanation.toDTO()
    }

    @QueryMapping
    fun explanationsForQuestion(
        @Argument questionId: UUID,
        @Argument status: ExplanationStatus?,
        @Argument languageCode: String?
    ): List<ExplanationDTO> {
        return explanationService.getExplanationsForQuestion(questionId, status, languageCode)
            .map { it.toDTO() }
    }

    @QueryMapping
    fun approvedExplanations(
        @Argument questionId: UUID,
        @Argument languageCode: String
    ): List<ExplanationDTO> {
        return explanationService.getApprovedExplanations(questionId, languageCode)
            .map { it.toDTO() }
    }

    @QueryMapping
    fun mySubmissions(): List<ExplanationDTO> {
        val userId = getCurrentUserId()
        return explanationService.getUserSubmissions(userId)
            .map { it.toDTO() }
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('PRIEST', 'THEOLOGY_REVIEWER', 'ADMIN')")
    fun moderationQueue(): List<ExplanationDTO> {
        return explanationService.getModerationQueue()
            .map { it.toDTO() }
    }

    @QueryMapping
    fun topVotedExplanations(
        @Argument questionId: UUID,
        @Argument limit: Int = 5
    ): List<ExplanationDTO> {
        return voteService.getTopVotedExplanations(questionId, limit)
            .map { it.toDTO() }
    }

    // =====================================================
    // Mutations
    // =====================================================

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    fun submitTextExplanation(@Argument input: SubmitTextExplanationInput): ExplanationDTO {
        val userId = getCurrentUserId()

        val explanation = explanationService.submitTextExplanation(
            questionId = input.questionId,
            submitterId = userId,
            languageCode = input.languageCode ?: "en",
            textContent = input.textContent
        )

        return explanation.toDTO()
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    fun submitFileExplanation(@Argument input: SubmitFileExplanationInput): ExplanationDTO {
        val userId = getCurrentUserId()

        val explanation = explanationService.submitFileExplanation(
            questionId = input.questionId,
            submitterId = userId,
            languageCode = input.languageCode ?: "en",
            contentType = input.contentType,
            fileId = input.fileId
        )

        return explanation.toDTO()
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteExplanation(@Argument id: UUID): Boolean {
        return explanationService.deleteExplanation(id)
    }

    // =====================================================
    // Schema Mappings (nested resolvers)
    // =====================================================

    @SchemaMapping(typeName = "Explanation", field = "voteStatistics")
    fun voteStatistics(explanation: ExplanationSubmission): VoteStatisticsDTO {
        val stats = voteService.getVoteStatistics(explanation.id)
        return VoteStatisticsDTO(
            totalVotes = stats.totalVotes,
            helpfulVotes = stats.helpfulVotes,
            unhelpfulVotes = stats.unhelpfulVotes,
            helpfulPercentage = stats.helpfulPercentage
        )
    }

    @SchemaMapping(typeName = "Explanation", field = "flagStatistics")
    fun flagStatistics(explanation: ExplanationSubmission): FlagStatisticsDTO {
        val stats = flagService.getFlagStatistics(explanation.id)
        return FlagStatisticsDTO(
            totalFlags = stats.totalFlags,
            openFlags = stats.openFlags,
            resolvedFlags = stats.resolvedFlags,
            dismissedFlags = stats.dismissedFlags
        )
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    private fun getCurrentUserId(): UUID {
        val authentication = SecurityContextHolder.getContext().authentication
        val email = authentication.principal as? String
            ?: throw IllegalStateException("User not authenticated")

        return userRepository.findByEmail(email)?.id
            ?: throw IllegalStateException("User not found")
    }

    private fun ExplanationSubmission.toDTO() = ExplanationDTO(
        id = this.id,
        questionId = this.question.id,
        submitterId = this.submitter.id,
        submitterName = this.submitter.name,
        languageCode = this.languageCode,
        contentType = this.contentType,
        textContent = this.textContent,
        fileUrl = this.fileUrl,
        fileSizeBytes = this.fileSizeBytes,
        fileMimeType = this.fileMimeType,
        durationSeconds = this.durationSeconds,
        submissionStatus = this.submissionStatus,
        qualityScore = this.qualityScore,
        viewCount = this.viewCount,
        helpfulCount = this.helpfulCount,
        submittedAt = this.submittedAt,
        reviewedAt = this.reviewedAt,
        approvedAt = this.approvedAt
    )
}

// =====================================================
// DTOs
// =====================================================

data class ExplanationDTO(
    val id: UUID,
    val questionId: UUID,
    val submitterId: UUID,
    val submitterName: String,
    val languageCode: String,
    val contentType: ExplanationContentType,
    val textContent: String?,
    val fileUrl: String?,
    val fileSizeBytes: Long?,
    val fileMimeType: String?,
    val durationSeconds: Int?,
    val submissionStatus: ExplanationStatus,
    val qualityScore: Int?,
    val viewCount: Int,
    val helpfulCount: Int,
    val submittedAt: java.time.Instant,
    val reviewedAt: java.time.Instant?,
    val approvedAt: java.time.Instant?
)

data class SubmitTextExplanationInput(
    val questionId: UUID,
    val languageCode: String?,
    val textContent: String
)

data class SubmitFileExplanationInput(
    val questionId: UUID,
    val languageCode: String?,
    val contentType: ExplanationContentType,
    val fileId: UUID
)

data class VoteStatisticsDTO(
    val totalVotes: Int,
    val helpfulVotes: Int,
    val unhelpfulVotes: Int,
    val helpfulPercentage: Double
)

data class FlagStatisticsDTO(
    val totalFlags: Int,
    val openFlags: Int,
    val resolvedFlags: Int,
    val dismissedFlags: Int
)