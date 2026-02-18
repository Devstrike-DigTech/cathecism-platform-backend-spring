package com.catechism.platform.repository.community

import com.catechism.platform.domain.community.*
import com.catechism.platform.domain.explanation.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID


@Repository
interface UserProfileRepository : JpaRepository<UserProfile, UUID> {
    fun findByUserId(userId: UUID): UserProfile?
    fun findByIsPublic(isPublic: Boolean): List<UserProfile>
}

@Repository
interface BadgeRepository : JpaRepository<Badge, UUID> {
    fun findByCode(code: String): Badge?
    fun findByBadgeCategoryAndIsActive(category: String, isActive: Boolean): List<Badge>
    fun findByIsActive(isActive: Boolean): List<Badge>
}

@Repository
interface UserBadgeRepository : JpaRepository<UserBadge, UUID> {
    fun findByUserId(userId: UUID): List<UserBadge>
    fun findByBadgeId(badgeId: UUID): List<UserBadge>
    fun findByUserIdAndBadgeId(userId: UUID, badgeId: UUID): UserBadge?
    fun countByUserId(userId: UUID): Long
}

@Repository
interface AchievementRepository : JpaRepository<Achievement, UUID> {
    fun findByCode(code: String): Achievement?
    fun findByIsActive(isActive: Boolean): List<Achievement>
    fun findByMetricKey(metricKey: String): List<Achievement>
    fun findByAchievementCategoryAndIsActive(category: String, isActive: Boolean): List<Achievement>
}

@Repository
interface UserAchievementRepository : JpaRepository<UserAchievement, UUID> {
    fun findByUserId(userId: UUID): List<UserAchievement>
    fun findByUserIdAndCompleted(userId: UUID, completed: Boolean): List<UserAchievement>
    fun findByUserIdAndAchievementId(userId: UUID, achievementId: UUID): UserAchievement?

    @Query("SELECT ua FROM UserAchievement ua WHERE ua.user.id = :userId AND ua.completed = true ORDER BY ua.completedAt DESC")
    fun findCompletedByUserId(@Param("userId") userId: UUID): List<UserAchievement>
}

@Repository
interface ContributionActivityRepository : JpaRepository<ContributionActivity, UUID> {
    fun findByUserId(userId: UUID): List<ContributionActivity>
    fun findByUserIdAndActivityType(userId: UUID, activityType: ActivityType): List<ContributionActivity>

    @Query("SELECT ca FROM ContributionActivity ca WHERE ca.user.id = :userId AND ca.activityDate >= :since ORDER BY ca.createdAt DESC")
    fun findByUserIdSince(@Param("userId") userId: UUID, @Param("since") since: java.time.LocalDate): List<ContributionActivity>

    @Query("SELECT SUM(ca.pointsEarned) FROM ContributionActivity ca WHERE ca.user.id = :userId")
    fun sumPointsByUserId(@Param("userId") userId: UUID): Long?
}

@Repository
interface LeaderboardEntryRepository : JpaRepository<LeaderboardEntry, UUID> {
    fun findByLeaderboardTypeAndPeriodKeyOrderByRank(type: LeaderboardType, periodKey: String): List<LeaderboardEntry>
    fun findByUserIdAndLeaderboardTypeAndPeriodKey(userId: UUID, type: LeaderboardType, periodKey: String): LeaderboardEntry?
    fun findByUserId(userId: UUID): List<LeaderboardEntry>
}