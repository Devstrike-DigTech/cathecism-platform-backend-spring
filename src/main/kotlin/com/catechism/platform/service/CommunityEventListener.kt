package com.catechism.platform.service

import com.catechism.platform.domain.community.ActivityType
import com.catechism.platform.repository.AppUserRepository
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.util.*

// =====================================================
// Domain Events
// =====================================================

data class ExplanationSubmittedEvent(val explanationId: UUID, val submitterId: UUID)
data class ExplanationApprovedEvent(val explanationId: UUID, val submitterId: UUID)
data class ExplanationVotedEvent(val explanationId: UUID, val voterId: UUID, val ownerId: UUID, val isHelpful: Boolean)
data class FlagResolvedEvent(val flagId: UUID, val moderatorId: UUID)

// =====================================================
// Event Listener — async so it never blocks main flow
// =====================================================

@Component
class CommunityEventListener(
    private val communityService: CommunityService
) {

    @Async
    @EventListener
    fun onExplanationSubmitted(event: ExplanationSubmittedEvent) {
        runCatching {
            communityService.recordActivity(
                userId = event.submitterId,
                activityType = ActivityType.SUBMISSION,
                entityType = "EXPLANATION",
                entityId = event.explanationId,
                pointsEarned = 5
            )
            // Check for first submission badge
            val profile = communityService.getOrCreateProfile(event.submitterId)
            if (profile.totalSubmissions == 1) {
                communityService.awardBadge(event.submitterId, "FIRST_SUBMISSION",
                    "Submitted your first explanation!")
            }
        }.onFailure { it.printStackTrace() }
    }

    @Async
    @EventListener
    fun onExplanationApproved(event: ExplanationApprovedEvent) {
        runCatching {
            communityService.recordApproval(event.submitterId, event.explanationId)

            val profile = communityService.getOrCreateProfile(event.submitterId)
            when (profile.approvedSubmissions) {
                1  -> communityService.awardBadge(event.submitterId, "FIRST_APPROVAL",
                    "Your first explanation was approved!")
                10 -> communityService.awardBadge(event.submitterId, "APPROVAL_10",
                    "10 approved explanations!")
                50 -> communityService.awardBadge(event.submitterId, "APPROVAL_50",
                    "50 approved explanations!")

                else -> {}
            }
        }.onFailure { it.printStackTrace() }
    }

    @Async
    @EventListener
    fun onExplanationVoted(event: ExplanationVotedEvent) {
        runCatching {
            // Record activity for the voter
            communityService.recordActivity(
                userId = event.voterId,
                activityType = ActivityType.VOTE,
                entityType = "EXPLANATION",
                entityId = event.explanationId,
                pointsEarned = 1
            )
            val voterProfile = communityService.getOrCreateProfile(event.voterId)
            if (voterProfile.totalVotesCast == 1) {
                communityService.awardBadge(event.voterId, "FIRST_VOTE", "Cast your first vote!")
            }

            // Record helpful vote received by owner
            if (event.isHelpful && event.voterId != event.ownerId) {
                communityService.recordHelpfulVoteReceived(event.ownerId)
                val ownerProfile = communityService.getOrCreateProfile(event.ownerId)
                when (ownerProfile.totalHelpfulVotes) {
                    10  -> communityService.awardBadge(event.ownerId, "HELPFUL_10",
                        "10 people found your explanations helpful!")
                    100 -> communityService.awardBadge(event.ownerId, "HELPFUL_100",
                        "100 people found your explanations helpful!")
                }
            }
        }.onFailure { it.printStackTrace() }
    }

    @Async
    @EventListener
    fun onFlagResolved(event: FlagResolvedEvent) {
        runCatching {
            communityService.recordActivity(
                userId = event.moderatorId,
                activityType = ActivityType.FLAG_RESOLVED,
                entityType = "FLAG",
                entityId = event.flagId,
                pointsEarned = 3
            )
            val profile = communityService.getOrCreateProfile(event.moderatorId)
            if (profile.totalFlagsResolved == 1) {
                communityService.awardBadge(event.moderatorId, "FIRST_REVIEW",
                    "Completed your first moderation action!")
            }
        }.onFailure { it.printStackTrace() }
    }
}