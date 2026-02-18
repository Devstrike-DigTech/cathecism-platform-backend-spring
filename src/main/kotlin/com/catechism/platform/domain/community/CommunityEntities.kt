package com.catechism.platform.domain.community

import com.catechism.platform.domain.AppUser
import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.*

// =====================================================
// USER PROFILE
// =====================================================

@Entity
@Table(name = "user_profile")
data class UserProfile(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: AppUser,

    @Column(columnDefinition = "TEXT")
    var bio: String? = null,

    @Column(name = "avatar_url")
    var avatarUrl: String? = null,

    @Column
    var location: String? = null,

    @Column(name = "website_url")
    var websiteUrl: String? = null,

    @Column(name = "display_name", length = 100)
    var displayName: String? = null,

    @Column(name = "is_public", nullable = false)
    var isPublic: Boolean = true,

    // Denormalised stats
    @Column(name = "total_submissions", nullable = false)
    var totalSubmissions: Int = 0,

    @Column(name = "approved_submissions", nullable = false)
    var approvedSubmissions: Int = 0,

    @Column(name = "total_votes_cast", nullable = false)
    var totalVotesCast: Int = 0,

    @Column(name = "total_helpful_votes", nullable = false)
    var totalHelpfulVotes: Int = 0,

    @Column(name = "total_flags_resolved", nullable = false)
    var totalFlagsResolved: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PreUpdate fun onUpdate() { updatedAt = Instant.now() }

    override fun equals(other: Any?) = other is UserProfile && id == other.id
    override fun hashCode() = id.hashCode()
}

// =====================================================
// BADGE
// =====================================================

@Entity
@Table(name = "badge")
data class Badge(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true, length = 50)
    val code: String,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(name = "icon_url")
    val iconUrl: String? = null,

    @Column(name = "badge_category", nullable = false, length = 50)
    val badgeCategory: String,

    @Column(name = "points_value", nullable = false)
    val pointsValue: Int = 0,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?) = other is Badge && id == other.id
    override fun hashCode() = id.hashCode()
}

// =====================================================
// USER BADGE (earned)
// =====================================================

@Entity
@Table(
    name = "user_badge",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "badge_id"])]
)
data class UserBadge(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: AppUser,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    val badge: Badge,

    @Column(name = "earned_at", nullable = false)
    val earnedAt: Instant = Instant.now(),

    @Column(name = "context_note", columnDefinition = "TEXT")
    val contextNote: String? = null
) {
    override fun equals(other: Any?) = other is UserBadge && id == other.id
    override fun hashCode() = id.hashCode()
}

// =====================================================
// ACHIEVEMENT
// =====================================================

@Entity
@Table(name = "achievement")
data class Achievement(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true, length = 50)
    val code: String,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(name = "icon_url")
    val iconUrl: String? = null,

    @Column(name = "achievement_category", nullable = false, length = 50)
    val achievementCategory: String,

    @Column(name = "metric_key", nullable = false, length = 100)
    val metricKey: String,

    @Column(name = "target_value", nullable = false)
    val targetValue: Int,

    @Column(name = "points_value", nullable = false)
    val pointsValue: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id")
    val badge: Badge? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?) = other is Achievement && id == other.id
    override fun hashCode() = id.hashCode()
}

// =====================================================
// USER ACHIEVEMENT (progress + completion)
// =====================================================

@Entity
@Table(
    name = "user_achievement",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "achievement_id"])]
)
data class UserAchievement(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: AppUser,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", nullable = false)
    val achievement: Achievement,

    @Column(name = "current_value", nullable = false)
    var currentValue: Int = 0,

    @Column(nullable = false)
    var completed: Boolean = false,

    @Column(name = "completed_at")
    var completedAt: Instant? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    val progressPercent: Int
        get() = ((currentValue.toDouble() / achievement.targetValue) * 100)
            .toInt().coerceIn(0, 100)

    @PreUpdate fun onUpdate() { updatedAt = Instant.now() }
    override fun equals(other: Any?) = other is UserAchievement && id == other.id
    override fun hashCode() = id.hashCode()
}

// =====================================================
// CONTRIBUTION ACTIVITY LOG
// =====================================================

@Entity
@Table(name = "contribution_activity")
data class ContributionActivity(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: AppUser,

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    val activityType: ActivityType,

    @Column(name = "entity_type", nullable = false, length = 50)
    val entityType: String,

    @Column(name = "entity_id", nullable = false)
    val entityId: UUID,

    @Column(name = "points_earned", nullable = false)
    val pointsEarned: Int = 0,

    @Column(name = "activity_date", nullable = false)
    val activityDate: LocalDate = LocalDate.now(),

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?) = other is ContributionActivity && id == other.id
    override fun hashCode() = id.hashCode()
}

enum class ActivityType {
    SUBMISSION,
    REVIEW,
    VOTE,
    FLAG_RESOLVED
}

// =====================================================
// LEADERBOARD ENTRY
// =====================================================

@Entity
@Table(
    name = "leaderboard_entry",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "leaderboard_type", "period_key"])]
)
data class LeaderboardEntry(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: AppUser,

    @Enumerated(EnumType.STRING)
    @Column(name = "leaderboard_type", nullable = false, length = 20)
    val leaderboardType: LeaderboardType,

    @Column(name = "period_key", nullable = false, length = 20)
    val periodKey: String,

    @Column(nullable = false)
    var rank: Int,

    @Column(name = "total_points", nullable = false)
    var totalPoints: Int = 0,

    @Column(nullable = false)
    var submissions: Int = 0,

    @Column(nullable = false)
    var approvals: Int = 0,

    @Column(name = "helpful_votes", nullable = false)
    var helpfulVotes: Int = 0,

    @Column(name = "snapshot_at", nullable = false)
    val snapshotAt: Instant = Instant.now()
) {
    override fun equals(other: Any?) = other is LeaderboardEntry && id == other.id
    override fun hashCode() = id.hashCode()
}

enum class LeaderboardType {
    WEEKLY,
    MONTHLY,
    ALL_TIME
}