package com.catechism.platform.graphql

import com.catechism.platform.domain.explanation.*
import com.catechism.platform.repository.AppUserRepository
import com.catechism.platform.service.FlagService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import java.util.*

@Controller
class FlagResolver(
    private val flagService: FlagService,
    private val userRepository: AppUserRepository
) {

    @QueryMapping
    fun flagsForExplanation(@Argument explanationId: UUID): List<ExplanationFlagDTO> {
        return flagService.getFlagsForExplanation(explanationId)
            .map { it.toDTO() }
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('PRIEST', 'THEOLOGY_REVIEWER', 'ADMIN')")
    fun openFlags(): List<ExplanationFlagDTO> {
        return flagService.getAllOpenFlags()
            .map { it.toDTO() }
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    fun flagExplanation(@Argument input: FlagExplanationInput): ExplanationFlagDTO {
        val userId = getCurrentUserId()

        val flag = flagService.flagExplanation(
            explanationId = input.explanationId,
            flaggerId = userId,
            flagReason = input.flagReason,
            flagDetails = input.flagDetails
        )

        return flag.toDTO()
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('PRIEST', 'THEOLOGY_REVIEWER', 'ADMIN')")
    fun resolveFlag(@Argument input: ResolveFlagInput): ExplanationFlagDTO {
        val moderatorId = getCurrentUserId()

        val flag = flagService.resolveFlag(
            flagId = input.flagId,
            moderatorId = moderatorId,
            resolution = input.resolution,
            notes = input.moderatorNotes
        )

        return flag.toDTO()
    }

    private fun getCurrentUserId(): UUID {
        val authentication = SecurityContextHolder.getContext().authentication
        val email = authentication.principal as? String
            ?: throw IllegalStateException("User not authenticated")

        return userRepository.findByEmail(email)?.id
            ?: throw IllegalStateException("User not found")
    }

    private fun ExplanationFlag.toDTO() = ExplanationFlagDTO(
        id = this.id,
        explanationId = this.explanation.id,
        flaggerId = this.flagger.id,
        flaggerName = this.flagger.name,
        flagReason = this.flagReason,
        flagDetails = this.flagDetails,
        flagStatus = this.flagStatus,
        moderatorId = this.moderator?.id,
        moderatorName = this.moderator?.name,
        moderatorNotes = this.moderatorNotes,
        resolvedAt = this.resolvedAt,
        createdAt = this.createdAt
    )
}

// DTOs
data class ExplanationFlagDTO(
    val id: UUID,
    val explanationId: UUID,
    val flaggerId: UUID,
    val flaggerName: String,
    val flagReason: FlagReason,
    val flagDetails: String?,
    val flagStatus: FlagStatus,
    val moderatorId: UUID?,
    val moderatorName: String?,
    val moderatorNotes: String?,
    val resolvedAt: java.time.Instant?,
    val createdAt: java.time.Instant
)

data class FlagExplanationInput(
    val explanationId: UUID,
    val flagReason: FlagReason,
    val flagDetails: String?
)

data class ResolveFlagInput(
    val flagId: UUID,
    val resolution: FlagStatus,
    val moderatorNotes: String
)