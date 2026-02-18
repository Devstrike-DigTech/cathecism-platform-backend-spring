package com.catechism.platform.graphql

import com.catechism.platform.domain.analytics.*
import com.catechism.platform.service.*
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class AnalyticsResolver(
    private val analyticsService: AnalyticsService
) {

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun dashboardSummary(): DashboardSummaryDTO =
        analyticsService.getDashboardSummary().toDTO()

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun explanationTrend(@Argument days: Int = 30): List<DailySnapshotDTO> =
        analyticsService.getExplanationTrend(days.coerceIn(1, 365)).map { it.toDTO() }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun userGrowthTrend(@Argument days: Int = 30): List<UserGrowthSnapshotDTO> =
        analyticsService.getUserGrowthTrend(days.coerceIn(1, 365)).map { it.toDTO() }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun moderationTrend(@Argument days: Int = 30): List<ModerationSnapshotDTO> =
        analyticsService.getModerationTrend(days.coerceIn(1, 365)).map { it.toDTO() }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun topExplanations(@Argument limit: Int = 10): List<TopContentEntryDTO> =
        analyticsService.getTopExplanations(limit.coerceIn(1, 50)).map { it.toDTO() }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun contentBreakdown(): ContentBreakdownDTO =
        analyticsService.getContentBreakdown().toDTO()

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun triggerAnalyticsSnapshot(): DashboardSummaryDTO =
        analyticsService.triggerSnapshot().toDTO()

    // =====================================================
    // Mappers
    // =====================================================

    private fun DashboardSummary.toDTO() = DashboardSummaryDTO(
        snapshotDate         = snapshotDate.toString(),
        totalQuestions       = totalQuestions,
        totalBooklets        = totalBooklets,
        totalUsers           = totalUsers,
        activeUsersToday     = activeUsersToday,
        newUsersToday        = newUsersToday,
        roleBreakdown        = roleBreakdown?.let {
            RoleBreakdownDTO(it.publicUsers, it.catechists, it.priests, it.theologyReviewers, it.admins)
        },
        totalExplanations    = totalExplanations,
        approvedExplanations = approvedExplanations,
        pendingExplanations  = pendingExplanations,
        newExplanationsToday = newExplanationsToday,
        newApprovalsToday    = newApprovalsToday,
        avgQualityScore      = avgQualityScore?.toDouble(),
        avgHelpfulPct        = avgHelpfulPct?.toDouble(),
        openFlags            = openFlags,
        moderationQueueLength    = moderationQueueLength,
        avgReviewHours       = avgReviewHours?.toDouble(),
        reviewsCompletedToday= reviewsCompletedToday
    )

    private fun AnalyticsDailySnapshot.toDTO() = DailySnapshotDTO(
        snapshotDate         = snapshotDate.toString(),
        totalExplanations    = totalExplanations,
        approvedExplanations = approvedExplanations,
        pendingExplanations  = pendingExplanations,
        newExplanationsToday = newExplanationsToday,
        newApprovalsToday    = newApprovalsToday,
        totalVotes           = totalVotes,
        totalFlags           = totalFlags,
        openFlags            = openFlags,
        avgQualityScore      = avgQualityScore?.toDouble(),
        avgHelpfulPct        = avgHelpfulPct?.toDouble(),
        activeUsersToday     = activeUsersToday,
        newUsersToday        = newUsersToday
    )

    private fun AnalyticsUserGrowth.toDTO() = UserGrowthSnapshotDTO(
        snapshotDate      = snapshotDate.toString(),
        totalUsers        = totalUsers,
        newRegistrations  = newRegistrations,
        publicUsers       = publicUsers,
        catechists        = catechists,
        priests           = priests,
        theologyReviewers = theologyReviewers,
        admins            = admins
    )

    private fun AnalyticsModerationPerformance.toDTO() = ModerationSnapshotDTO(
        snapshotDate          = snapshotDate.toString(),
        avgReviewHours        = avgReviewHours?.toDouble(),
        queueLength           = queueLength,
        reviewsCompletedToday = reviewsCompletedToday,
        flagsResolvedToday    = flagsResolvedToday
    )

    private fun TopContentEntry.toDTO() = TopContentEntryDTO(
        rank        = rank,
        entityId    = entityId,
        entityType  = entityType,
        label       = label,
        metricKey   = metricKey,
        metricValue = metricValue.toDouble()
    )

    private fun ContentBreakdown.toDTO() = ContentBreakdownDTO(
        byStatus   = byStatus.map   { ContentBreakdownEntryDTO(it.label, it.count) },
        byType     = byType.map     { ContentBreakdownEntryDTO(it.label, it.count) },
        byLanguage = byLanguage.map { ContentBreakdownEntryDTO(it.label, it.count) }
    )
}

// =====================================================
// DTOs
// =====================================================

data class DashboardSummaryDTO(
    val snapshotDate: String,
    val totalQuestions: Int,
    val totalBooklets: Int,
    val totalUsers: Int,
    val activeUsersToday: Int,
    val newUsersToday: Int,
    val roleBreakdown: RoleBreakdownDTO?,
    val totalExplanations: Int,
    val approvedExplanations: Int,
    val pendingExplanations: Int,
    val newExplanationsToday: Int,
    val newApprovalsToday: Int,
    val avgQualityScore: Double?,
    val avgHelpfulPct: Double?,
    val openFlags: Int,
    val moderationQueueLength: Int,
    val avgReviewHours: Double?,
    val reviewsCompletedToday: Int
)

data class RoleBreakdownDTO(
    val publicUsers: Int,
    val catechists: Int,
    val priests: Int,
    val theologyReviewers: Int,
    val admins: Int
)

data class DailySnapshotDTO(
    val snapshotDate: String,
    val totalExplanations: Int,
    val approvedExplanations: Int,
    val pendingExplanations: Int,
    val newExplanationsToday: Int,
    val newApprovalsToday: Int,
    val totalVotes: Int,
    val totalFlags: Int,
    val openFlags: Int,
    val avgQualityScore: Double?,
    val avgHelpfulPct: Double?,
    val activeUsersToday: Int,
    val newUsersToday: Int
)

data class UserGrowthSnapshotDTO(
    val snapshotDate: String,
    val totalUsers: Int,
    val newRegistrations: Int,
    val publicUsers: Int,
    val catechists: Int,
    val priests: Int,
    val theologyReviewers: Int,
    val admins: Int
)

data class ModerationSnapshotDTO(
    val snapshotDate: String,
    val avgReviewHours: Double?,
    val queueLength: Int,
    val reviewsCompletedToday: Int,
    val flagsResolvedToday: Int
)

data class TopContentEntryDTO(
    val rank: Int,
    val entityId: java.util.UUID,
    val entityType: String,
    val label: String,
    val metricKey: String,
    val metricValue: Double
)

data class ContentBreakdownDTO(
    val byStatus: List<ContentBreakdownEntryDTO>,
    val byType: List<ContentBreakdownEntryDTO>,
    val byLanguage: List<ContentBreakdownEntryDTO>
)

data class ContentBreakdownEntryDTO(val label: String, val count: Int)