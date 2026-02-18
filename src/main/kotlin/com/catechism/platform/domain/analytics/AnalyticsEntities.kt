package com.catechism.platform.domain.analytics

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "analytics_daily_snapshot")
data class AnalyticsDailySnapshot(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "snapshot_date", nullable = false, unique = true)
    val snapshotDate: LocalDate,

    // Content counts
    @Column(name = "total_questions", nullable = false) var totalQuestions: Int = 0,
    @Column(name = "total_booklets",  nullable = false) var totalBooklets:  Int = 0,
    @Column(name = "total_acts",      nullable = false) var totalActs:      Int = 0,

    // Explanation counts
    @Column(name = "total_explanations",     nullable = false) var totalExplanations:    Int = 0,
    @Column(name = "pending_explanations",   nullable = false) var pendingExplanations:  Int = 0,
    @Column(name = "approved_explanations",  nullable = false) var approvedExplanations: Int = 0,
    @Column(name = "rejected_explanations",  nullable = false) var rejectedExplanations: Int = 0,
    @Column(name = "flagged_explanations",   nullable = false) var flaggedExplanations:  Int = 0,
    @Column(name = "new_explanations_today", nullable = false) var newExplanationsToday: Int = 0,
    @Column(name = "new_approvals_today",    nullable = false) var newApprovalsToday:    Int = 0,

    // User counts
    @Column(name = "total_users",       nullable = false) var totalUsers:      Int = 0,
    @Column(name = "new_users_today",   nullable = false) var newUsersToday:   Int = 0,
    @Column(name = "active_users_today",nullable = false) var activeUsersToday:Int = 0,

    // Engagement
    @Column(name = "total_votes",        nullable = false) var totalVotes:       Int = 0,
    @Column(name = "total_helpful_votes",nullable = false) var totalHelpfulVotes:Int = 0,
    @Column(name = "total_flags",        nullable = false) var totalFlags:       Int = 0,
    @Column(name = "open_flags",         nullable = false) var openFlags:        Int = 0,
    @Column(name = "total_reviews",      nullable = false) var totalReviews:     Int = 0,

    // Quality
    @Column(name = "avg_quality_score", precision = 5, scale = 2) var avgQualityScore: BigDecimal? = null,
    @Column(name = "avg_helpful_pct",   precision = 5, scale = 2) var avgHelpfulPct:   BigDecimal? = null,

    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?) = other is AnalyticsDailySnapshot && id == other.id
    override fun hashCode() = id.hashCode()
}

@Entity
@Table(name = "analytics_user_growth")
data class AnalyticsUserGrowth(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "snapshot_date", nullable = false, unique = true) val snapshotDate: LocalDate,
    @Column(name = "total_users",          nullable = false) var totalUsers:         Int = 0,
    @Column(name = "public_users",         nullable = false) var publicUsers:        Int = 0,
    @Column(name = "catechists",           nullable = false) var catechists:         Int = 0,
    @Column(name = "priests",             nullable = false) var priests:            Int = 0,
    @Column(name = "theology_reviewers",   nullable = false) var theologyReviewers:  Int = 0,
    @Column(name = "admins",              nullable = false) var admins:             Int = 0,
    @Column(name = "new_registrations",   nullable = false) var newRegistrations:   Int = 0
) {
    override fun equals(other: Any?) = other is AnalyticsUserGrowth && id == other.id
    override fun hashCode() = id.hashCode()
}

@Entity
@Table(name = "analytics_moderation_performance")
data class AnalyticsModerationPerformance(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "snapshot_date",          nullable = false, unique = true) val snapshotDate: LocalDate,
    @Column(name = "avg_review_hours",    precision = 8, scale = 2) var avgReviewHours:    BigDecimal? = null,
    @Column(name = "median_review_hours", precision = 8, scale = 2) var medianReviewHours: BigDecimal? = null,
    @Column(name = "queue_length",           nullable = false) var queueLength:          Int = 0,
    @Column(name = "reviews_completed_today",nullable = false) var reviewsCompletedToday:Int = 0,
    @Column(name = "flags_resolved_today",   nullable = false) var flagsResolvedToday:   Int = 0
) {
    override fun equals(other: Any?) = other is AnalyticsModerationPerformance && id == other.id
    override fun hashCode() = id.hashCode()
}