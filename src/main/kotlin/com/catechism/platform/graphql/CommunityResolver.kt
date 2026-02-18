package com.catechism.platform.graphql

import com.catechism.platform.domain.community.*
import com.catechism.platform.domain.community.LeaderboardType
import com.catechism.platform.domain.community.UserBadge
import com.catechism.platform.domain.community.UserProfile
import com.catechism.platform.repository.AppUserRepository
import com.catechism.platform.repository.community.UserBadgeRepository
import com.catechism.platform.repository.community.UserProfileRepository
import com.catechism.platform.service.CommunityService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import java.util.*

@Controller
class CommunityResolver(
    private val communityService: CommunityService,
    private val userRepository: AppUserRepository,
    private val profileRepository: UserProfileRepository,
    private val userBadgeRepository: UserBadgeRepository
) {

    // =====================================================
    // Queries
    // =====================================================

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun myProfile(): UserProfileDTO {
        val userId = getCurrentUserId()
        val profile = communityService.getOrCreateProfile(userId)
        return profile.toDTO(communityService)
    }

    @QueryMapping
    fun userProfile(@Argument userId: UUID): UserProfileDTO? {
        val profile = communityService.getProfile(userId) ?: return null
        if (!profile.isPublic) {
            // Only return private profiles to the owner
            val currentUserId = runCatching { getCurrentUserId() }.getOrNull()
            if (currentUserId != userId) return null
        }
        return profile.toDTO(communityService)
    }

    @QueryMapping
    fun leaderboard(
        @Argument type: LeaderboardType,
        @Argument limit: Int = 20
    ): List<LeaderboardEntryDTO> {
        return communityService.getLeaderboard(type, limit).map { it.toDTO(userBadgeRepository) }
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun myRank(@Argument type: LeaderboardType): LeaderboardEntryDTO? {
        val userId = getCurrentUserId()
        return communityService.getUserRank(userId, type)?.toDTO(userBadgeRepository)
    }

    @QueryMapping
    fun allBadges(): List<BadgeDTO> = communityService.getAllBadges().map { it.toDTO() }

    @QueryMapping
    fun allAchievements(): List<AchievementDTO> = communityService.getAllAchievements().map { it.toDTO() }

    // =====================================================
    // Mutations
    // =====================================================

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    fun updateProfile(@Argument input: UpdateProfileInput): UserProfileDTO {
        val userId = getCurrentUserId()
        val profile = communityService.updateProfile(
            userId = userId,
            bio = input.bio,
            avatarUrl = input.avatarUrl,
            location = input.location,
            websiteUrl = input.websiteUrl,
            displayName = input.displayName,
            isPublic = input.isPublic
        )
        return profile.toDTO(communityService)
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun rebuildLeaderboard(@Argument type: LeaderboardType): Boolean {
        communityService.rebuildLeaderboard(type)
        return true
    }

    // =====================================================
    // Helpers
    // =====================================================

    private fun getCurrentUserId(): UUID {
        val email = SecurityContextHolder.getContext().authentication.principal as? String
            ?: throw IllegalStateException("User not authenticated")
        return userRepository.findByEmail(email)?.id
            ?: throw IllegalStateException("User not found")
    }

    private fun UserProfile.toDTO(svc: CommunityService) = UserProfileDTO(
        id = id,
        userId = user.id,
        displayName = displayName,
        bio = bio,
        avatarUrl = avatarUrl,
        location = location,
        websiteUrl = websiteUrl,
        isPublic = isPublic,
        totalSubmissions = totalSubmissions,
        approvedSubmissions = approvedSubmissions,
        totalVotesCast = totalVotesCast,
        totalHelpfulVotes = totalHelpfulVotes,
        totalPoints = svc.getTotalPoints(user.id),
        badges = svc.getUserBadges(user.id).map { it.toDTO() },
        achievements = svc.getUserAchievements(user.id).map { it.toDTO() },
        recentActivity = svc.getRecentActivity(user.id).map { it.toDTO() }
    )

    private fun UserBadge.toDTO() = UserBadgeDTO(
        id = id,
        badge = badge.toDTO(),
        earnedAt = earnedAt,
        contextNote = contextNote
    )

    private fun Badge.toDTO() = BadgeDTO(
        id = id, code = code, name = name, description = description,
        iconUrl = iconUrl, badgeCategory = badgeCategory, pointsValue = pointsValue
    )

    private fun UserAchievement.toDTO() = UserAchievementDTO(
        id = id,
        achievement = achievement.toDTO(),
        currentValue = currentValue,
        progressPercent = progressPercent,
        completed = completed,
        completedAt = completedAt
    )

    private fun Achievement.toDTO() = AchievementDTO(
        id = id, code = code, name = name, description = description,
        iconUrl = iconUrl, achievementCategory = achievementCategory,
        metricKey = metricKey, targetValue = targetValue, pointsValue = pointsValue
    )

    private fun ContributionActivity.toDTO() = ContributionActivityDTO(
        id = id,
        activityType = activityType.name,
        entityType = entityType,
        entityId = entityId,
        pointsEarned = pointsEarned,
        activityDate = activityDate.toString(),
        createdAt = createdAt
    )

    private fun LeaderboardEntry.toDTO(badgeRepo: UserBadgeRepository) = LeaderboardEntryDTO(
        rank = rank,
        userId = user.id,
        displayName = profileRepository.findByUserId(user.id)?.displayName ?: user.name,
        totalPoints = totalPoints,
        submissions = submissions,
        approvals = approvals,
        helpfulVotes = helpfulVotes,
        badgeCount = badgeRepo.countByUserId(user.id).toInt()
    )
}

// =====================================================
// DTOs
// =====================================================

data class UserProfileDTO(
    val id: UUID,
    val userId: UUID,
    val displayName: String?,
    val bio: String?,
    val avatarUrl: String?,
    val location: String?,
    val websiteUrl: String?,
    val isPublic: Boolean,
    val totalSubmissions: Int,
    val approvedSubmissions: Int,
    val totalVotesCast: Int,
    val totalHelpfulVotes: Int,
    val totalPoints: Long,
    val badges: List<UserBadgeDTO>,
    val achievements: List<UserAchievementDTO>,
    val recentActivity: List<ContributionActivityDTO>
) {
    val badgeCount: Int get() = badges.size
}

data class BadgeDTO(
    val id: UUID, val code: String, val name: String,
    val description: String, val iconUrl: String?,
    val badgeCategory: String, val pointsValue: Int
)

data class UserBadgeDTO(
    val id: UUID, val badge: BadgeDTO,
    val earnedAt: java.time.Instant, val contextNote: String?
)

data class AchievementDTO(
    val id: UUID, val code: String, val name: String,
    val description: String, val iconUrl: String?,
    val achievementCategory: String, val metricKey: String,
    val targetValue: Int, val pointsValue: Int
)

data class UserAchievementDTO(
    val id: UUID, val achievement: AchievementDTO,
    val currentValue: Int, val progressPercent: Int,
    val completed: Boolean, val completedAt: java.time.Instant?
)

data class ContributionActivityDTO(
    val id: UUID, val activityType: String, val entityType: String,
    val entityId: UUID, val pointsEarned: Int,
    val activityDate: String, val createdAt: java.time.Instant
)

data class LeaderboardEntryDTO(
    val rank: Int, val userId: UUID, val displayName: String?,
    val totalPoints: Int, val submissions: Int,
    val approvals: Int, val helpfulVotes: Int, val badgeCount: Int
)

data class UpdateProfileInput(
    val bio: String? = null,
    val avatarUrl: String? = null,
    val location: String? = null,
    val websiteUrl: String? = null,
    val displayName: String? = null,
    val isPublic: Boolean? = null
)