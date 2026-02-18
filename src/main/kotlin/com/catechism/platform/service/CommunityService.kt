package com.catechism.platform.service

import com.catechism.platform.domain.community.*
import com.catechism.platform.repository.AppUserRepository
import com.catechism.platform.repository.community.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Service
@Transactional
class CommunityService(
    private val userRepository: AppUserRepository,
    private val profileRepository: UserProfileRepository,
    private val badgeRepository: BadgeRepository,
    private val userBadgeRepository: UserBadgeRepository,
    private val achievementRepository: AchievementRepository,
    private val userAchievementRepository: UserAchievementRepository,
    private val activityRepository: ContributionActivityRepository,
    private val leaderboardRepository: LeaderboardEntryRepository
) {

    // =====================================================
    // PROFILES
    // =====================================================

    fun getOrCreateProfile(userId: UUID): UserProfile {
        return profileRepository.findByUserId(userId) ?: run {
            val user = userRepository.findById(userId).orElseThrow {
                IllegalArgumentException("User not found: $userId")
            }
            profileRepository.save(UserProfile(user = user))
        }
    }

    fun getProfile(userId: UUID): UserProfile? = profileRepository.findByUserId(userId)

    fun updateProfile(
        userId: UUID,
        bio: String? = null,
        avatarUrl: String? = null,
        location: String? = null,
        websiteUrl: String? = null,
        displayName: String? = null,
        isPublic: Boolean? = null
    ): UserProfile {
        val profile = getOrCreateProfile(userId)
        bio?.let { profile.bio = it }
        avatarUrl?.let { profile.avatarUrl = it }
        location?.let { profile.location = it }
        websiteUrl?.let { profile.websiteUrl = it }
        displayName?.let { profile.displayName = it }
        isPublic?.let { profile.isPublic = it }
        return profileRepository.save(profile)
    }

    // =====================================================
    // ACTIVITY TRACKING
    // =====================================================

    fun recordActivity(
        userId: UUID,
        activityType: ActivityType,
        entityType: String,
        entityId: UUID,
        pointsEarned: Int = 0
    ): ContributionActivity {
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found: $userId")
        }
        val activity = ContributionActivity(
            user = user,
            activityType = activityType,
            entityType = entityType,
            entityId = entityId,
            pointsEarned = pointsEarned
        )
        val saved = activityRepository.save(activity)

        // Update profile stats and check achievements
        updateProfileStats(userId, activityType)
        checkAndAwardAchievements(userId)

        return saved
    }

    private fun updateProfileStats(userId: UUID, activityType: ActivityType) {
        val profile = getOrCreateProfile(userId)
        when (activityType) {
            ActivityType.SUBMISSION  -> profile.totalSubmissions++
            ActivityType.VOTE        -> profile.totalVotesCast++
            ActivityType.REVIEW      -> { /* handled separately */ }
            ActivityType.FLAG_RESOLVED -> profile.totalFlagsResolved++
        }
        profileRepository.save(profile)
    }

    fun recordApproval(userId: UUID, explanationId: UUID) {
        val profile = getOrCreateProfile(userId)
        profile.approvedSubmissions++
        profileRepository.save(profile)
        recordActivity(userId, ActivityType.SUBMISSION, "EXPLANATION", explanationId, pointsEarned = 15)
        checkAndAwardAchievements(userId)
    }

    fun recordHelpfulVoteReceived(userId: UUID) {
        val profile = getOrCreateProfile(userId)
        profile.totalHelpfulVotes++
        profileRepository.save(profile)
        checkAndAwardAchievements(userId)
    }

    // =====================================================
    // BADGES
    // =====================================================

    fun getAllBadges(): List<Badge> = badgeRepository.findByIsActive(true)

    fun getUserBadges(userId: UUID): List<UserBadge> =
        userBadgeRepository.findByUserId(userId)
            .sortedByDescending { it.earnedAt }

    fun awardBadge(userId: UUID, badgeCode: String, contextNote: String? = null): UserBadge? {
        val user = userRepository.findById(userId).orElse(null) ?: return null
        val badge = badgeRepository.findByCode(badgeCode) ?: return null

        // Already has it?
        if (userBadgeRepository.findByUserIdAndBadgeId(userId, badge.id) != null) return null

        return userBadgeRepository.save(
            UserBadge(user = user, badge = badge, contextNote = contextNote)
        )
    }

    fun hasbadge(userId: UUID, badgeCode: String): Boolean {
        val badge = badgeRepository.findByCode(badgeCode) ?: return false
        return userBadgeRepository.findByUserIdAndBadgeId(userId, badge.id) != null
    }

    // =====================================================
    // ACHIEVEMENTS
    // =====================================================

    fun getAllAchievements(): List<Achievement> = achievementRepository.findByIsActive(true)

    fun getUserAchievements(userId: UUID): List<UserAchievement> =
        userAchievementRepository.findByUserId(userId)
            .sortedWith(compareByDescending<UserAchievement> { it.completed }
                .thenByDescending { it.currentValue.toDouble() / it.achievement.targetValue })

    fun getCompletedAchievements(userId: UUID): List<UserAchievement> =
        userAchievementRepository.findCompletedByUserId(userId)

    fun checkAndAwardAchievements(userId: UUID): List<UserAchievement> {
        val profile = profileRepository.findByUserId(userId) ?: return emptyList()
        val allAchievements = achievementRepository.findByIsActive(true)
        val newlyCompleted = mutableListOf<UserAchievement>()

        val metricValues = mapOf(
            "total_submissions"    to profile.totalSubmissions,
            "approved_submissions" to profile.approvedSubmissions,
            "total_votes_cast"     to profile.totalVotesCast,
            "total_helpful_votes"  to profile.totalHelpfulVotes,
            "reviews_completed"    to profile.totalFlagsResolved, // reuse for now
        )

        for (achievement in allAchievements) {
            val currentMetric = metricValues[achievement.metricKey] ?: continue

            val userAchievement = userAchievementRepository
                .findByUserIdAndAchievementId(userId, achievement.id)

            if (userAchievement == null) {
                // Create progress record
                val ua = UserAchievement(
                    user = userRepository.findById(userId).get(),
                    achievement = achievement,
                    currentValue = currentMetric,
                    completed = currentMetric >= achievement.targetValue,
                    completedAt = if (currentMetric >= achievement.targetValue) Instant.now() else null
                )
                val saved = userAchievementRepository.save(ua)
                if (saved.completed) {
                    newlyCompleted.add(saved)
                    // Award associated badge if any
                    achievement.badge?.let { awardBadge(userId, it.code, "Earned via ${achievement.name}") }
                }
            } else if (!userAchievement.completed) {
                userAchievement.currentValue = currentMetric
                if (currentMetric >= achievement.targetValue) {
                    userAchievement.completed = true
                    userAchievement.completedAt = Instant.now()
                    userAchievementRepository.save(userAchievement)
                    newlyCompleted.add(userAchievement)
                    achievement.badge?.let { awardBadge(userId, it.code, "Earned via ${achievement.name}") }
                } else {
                    userAchievementRepository.save(userAchievement)
                }
            }
        }

        return newlyCompleted
    }

    // =====================================================
    // LEADERBOARDS
    // =====================================================

    fun getLeaderboard(type: LeaderboardType, limit: Int = 20): List<LeaderboardEntry> {
        val periodKey = currentPeriodKey(type)
        return leaderboardRepository
            .findByLeaderboardTypeAndPeriodKeyOrderByRank(type, periodKey)
            .take(limit)
    }

    fun getUserRank(userId: UUID, type: LeaderboardType): LeaderboardEntry? {
        val periodKey = currentPeriodKey(type)
        return leaderboardRepository.findByUserIdAndLeaderboardTypeAndPeriodKey(userId, type, periodKey)
    }

    fun rebuildLeaderboard(type: LeaderboardType) {
        val periodKey = currentPeriodKey(type)
        val since = when (type) {
            LeaderboardType.WEEKLY  -> LocalDate.now().minusWeeks(1)
            LeaderboardType.MONTHLY -> LocalDate.now().minusMonths(1)
            LeaderboardType.ALL_TIME -> LocalDate.of(2000, 1, 1)
        }

        // Score all users with activity in the period
        val userIds = activityRepository.findAll()
            .filter { it.activityDate >= since }
            .map { it.user.id }
            .distinct()

        val scored = userIds.mapNotNull { userId ->
            val profile = profileRepository.findByUserId(userId) ?: return@mapNotNull null
            val points = activityRepository.findByUserIdSince(userId, since)
                .sumOf { it.pointsEarned }

            Triple(userId, profile, points)
        }.sortedByDescending { it.third }

        // Upsert ranked entries
        scored.forEachIndexed { index, (userId, profile, points) ->
            val user = userRepository.findById(userId).get()
            val existing = leaderboardRepository
                .findByUserIdAndLeaderboardTypeAndPeriodKey(userId, type, periodKey)

            if (existing != null) {
                existing.rank = index + 1
                existing.totalPoints = points
                existing.submissions = profile.totalSubmissions
                existing.approvals = profile.approvedSubmissions
                existing.helpfulVotes = profile.totalHelpfulVotes
                leaderboardRepository.save(existing)
            } else {
                leaderboardRepository.save(
                    LeaderboardEntry(
                        user = user,
                        leaderboardType = type,
                        periodKey = periodKey,
                        rank = index + 1,
                        totalPoints = points,
                        submissions = profile.totalSubmissions,
                        approvals = profile.approvedSubmissions,
                        helpfulVotes = profile.totalHelpfulVotes
                    )
                )
            }
        }
    }

    // =====================================================
    // STATS
    // =====================================================

    fun getTotalPoints(userId: UUID): Long =
        activityRepository.sumPointsByUserId(userId) ?: 0L

    fun getRecentActivity(userId: UUID, days: Int = 30): List<ContributionActivity> {
        val since = LocalDate.now().minusDays(days.toLong())
        return activityRepository.findByUserIdSince(userId, since)
            .sortedByDescending { it.createdAt }
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private fun currentPeriodKey(type: LeaderboardType): String = when (type) {
        LeaderboardType.WEEKLY  -> {
            val now = LocalDate.now()
            "${now.year}-W${now.format(DateTimeFormatter.ofPattern("ww"))}"
        }
        LeaderboardType.MONTHLY -> LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        LeaderboardType.ALL_TIME -> "ALL"
    }
}