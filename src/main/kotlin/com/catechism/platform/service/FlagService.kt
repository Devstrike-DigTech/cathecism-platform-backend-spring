package com.catechism.platform.service

import com.catechism.platform.domain.*
import com.catechism.platform.domain.explanation.ExplanationFlag
import com.catechism.platform.domain.explanation.ExplanationStatus
import com.catechism.platform.domain.explanation.FlagReason
import com.catechism.platform.domain.explanation.FlagStatus
import com.catechism.platform.repository.*
import com.catechism.platform.repository.explanation.ExplanationFlagRepository
import com.catechism.platform.repository.explanation.ExplanationSubmissionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class FlagService(
    private val flagRepository: ExplanationFlagRepository,
    private val explanationRepository: ExplanationSubmissionRepository,
    private val userRepository: AppUserRepository,
    private val explanationService: ExplanationService
) {

    /**
     * Flag an explanation
     */
    fun flagExplanation(
        explanationId: UUID,
        flaggerId: UUID,
        flagReason: FlagReason,
        flagDetails: String?
    ): ExplanationFlag {
        // Validate explanation exists
        val explanation = explanationRepository.findById(explanationId).orElseThrow {
            IllegalArgumentException("Explanation not found: $explanationId")
        }

        // Validate flagger exists
        val flagger = userRepository.findById(flaggerId).orElseThrow {
            IllegalArgumentException("User not found: $flaggerId")
        }

        // Check if user already flagged this explanation
        val existingFlag = flagRepository.findByExplanationIdAndFlaggerId(explanationId, flaggerId)
        if (existingFlag != null) {
            throw IllegalArgumentException("User has already flagged this explanation")
        }

        // Create flag
        val flag = ExplanationFlag(
            explanation = explanation,
            flagger = flagger,
            flagReason = flagReason,
            flagDetails = flagDetails,
            flagStatus = FlagStatus.OPEN
        )

        val savedFlag = flagRepository.save(flag)

        // If explanation was approved, change status to FLAGGED
        if (explanation.submissionStatus == ExplanationStatus.APPROVED) {
            explanationService.updateStatus(explanationId, ExplanationStatus.FLAGGED)
        }

        // Recalculate quality score (flags affect score)
        explanationService.updateQualityScore(explanationId)

        return savedFlag
    }

    /**
     * Resolve a flag
     */
    fun resolveFlag(
        flagId: UUID,
        moderatorId: UUID,
        resolution: FlagStatus,
        notes: String
    ): ExplanationFlag {
        require(resolution in listOf(FlagStatus.RESOLVED, FlagStatus.DISMISSED)) {
            "Resolution must be RESOLVED or DISMISSED"
        }

        // Validate flag exists
        val flag = flagRepository.findById(flagId).orElseThrow {
            IllegalArgumentException("Flag not found: $flagId")
        }

        // Validate moderator exists
        val moderator = userRepository.findById(moderatorId).orElseThrow {
            IllegalArgumentException("Moderator not found: $moderatorId")
        }

        // Validate moderator has permission
        require(moderator.role.name in listOf("PRIEST", "THEOLOGY_REVIEWER", "ADMIN")) {
            "User does not have permission to resolve flags"
        }

        // Resolve the flag
        flag.resolve(moderator, notes, resolution)
        val savedFlag = flagRepository.save(flag)

        // Check if all flags for this explanation are resolved
        val explanation = flag.explanation
        val openFlags = flagRepository.findByExplanationId(explanation.id)
            .filter { it.flagStatus == FlagStatus.OPEN }

        // If no more open flags and explanation was flagged, restore to approved
        if (openFlags.isEmpty() && explanation.submissionStatus == ExplanationStatus.FLAGGED) {
            explanationService.updateStatus(explanation.id, ExplanationStatus.APPROVED)
        }

        // Recalculate quality score
        explanationService.updateQualityScore(explanation.id)

        return savedFlag
    }

    /**
     * Get flags for an explanation
     */
    fun getFlagsForExplanation(explanationId: UUID): List<ExplanationFlag> {
        return flagRepository.findByExplanationId(explanationId)
            .sortedByDescending { it.createdAt }
    }

    /**
     * Get open flags for an explanation
     */
    fun getOpenFlagsForExplanation(explanationId: UUID): List<ExplanationFlag> {
        return flagRepository.findByExplanationId(explanationId)
            .filter { it.flagStatus == FlagStatus.OPEN }
            .sortedByDescending { it.createdAt }
    }

    /**
     * Get flags by a user
     */
    fun getFlagsByUser(userId: UUID): List<ExplanationFlag> {
        return flagRepository.findByFlaggerId(userId)
            .sortedByDescending { it.createdAt }
    }

    /**
     * Get all open flags (for moderation)
     */
    fun getAllOpenFlags(): List<ExplanationFlag> {
        return flagRepository.findByFlagStatus(FlagStatus.OPEN)
            .sortedBy { it.createdAt } // Oldest first
    }

    /**
     * Get flags resolved by a moderator
     */
    fun getFlagsResolvedByModerator(moderatorId: UUID): List<ExplanationFlag> {
        return flagRepository.findByModeratorId(moderatorId)
            .sortedByDescending { it.resolvedAt }
    }

    /**
     * Get flag statistics for an explanation
     */
    fun getFlagStatistics(explanationId: UUID): FlagStatistics {
        val flags = flagRepository.findByExplanationId(explanationId)

        val reasonCounts = flags.groupingBy { it.flagReason }.eachCount()
        val statusCounts = flags.groupingBy { it.flagStatus }.eachCount()

        return FlagStatistics(
            totalFlags = flags.size,
            openFlags = statusCounts[FlagStatus.OPEN] ?: 0,
            resolvedFlags = statusCounts[FlagStatus.RESOLVED] ?: 0,
            dismissedFlags = statusCounts[FlagStatus.DISMISSED] ?: 0,
            flagReasonCounts = reasonCounts
        )
    }

    /**
     * Check if explanation is heavily flagged
     */
    fun isHeavilyFlagged(explanationId: UUID, threshold: Int = 3): Boolean {
        val openFlags = getOpenFlagsForExplanation(explanationId)
        return openFlags.size >= threshold
    }
}

/**
 * Flag statistics
 */
data class FlagStatistics(
    val totalFlags: Int,
    val openFlags: Int,
    val resolvedFlags: Int,
    val dismissedFlags: Int,
    val flagReasonCounts: Map<FlagReason, Int>
)