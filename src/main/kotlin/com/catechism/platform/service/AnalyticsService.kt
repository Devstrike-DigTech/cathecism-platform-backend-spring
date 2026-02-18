package com.catechism.platform.service

import com.catechism.platform.domain.analytics.*
import com.catechism.platform.domain.explanation.*
import com.catechism.platform.repository.*
import com.catechism.platform.repository.analytics.AnalyticsDailySnapshotRepository
import com.catechism.platform.repository.analytics.AnalyticsModerationPerformanceRepository
import com.catechism.platform.repository.analytics.AnalyticsUserGrowthRepository
import com.catechism.platform.repository.community.*
import com.catechism.platform.repository.explanation.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class AnalyticsService(
    private val snapshotRepository: AnalyticsDailySnapshotRepository,
    private val userGrowthRepository: AnalyticsUserGrowthRepository,
    private val moderationRepository: AnalyticsModerationPerformanceRepository,
    private val userRepository: AppUserRepository,
    private val bookletRepository: CatechismBookletRepository,
    private val questionRepository: CatechismQuestionRepository,
    private val actRepository: CatechismActRepository,
    private val explanationRepository: ExplanationSubmissionRepository,
    private val reviewRepository: ExplanationReviewRepository,
    private val flagRepository: ExplanationFlagRepository,
    private val voteRepository: ExplanationVoteRepository,
    private val activityRepository: ContributionActivityRepository
) {

    // =====================================================
    // SCHEDULED SNAPSHOT — runs every night at 01:00
    // =====================================================

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    fun buildDailySnapshot() {
        val today = LocalDate.now()
        buildSnapshotForDate(today)
        buildUserGrowthForDate(today)
        buildModerationPerformanceForDate(today)
    }

    @Transactional
    fun buildSnapshotForDate(date: LocalDate): AnalyticsDailySnapshot {
        val existing = snapshotRepository.findBySnapshotDate(date)
            ?: AnalyticsDailySnapshot(snapshotDate = date)

        val yesterday = date.minusDays(1)
        val allExplanations = explanationRepository.findAll()
        val todayExplanations = allExplanations.filter {
            it.submittedAt.atZone(java.time.ZoneOffset.UTC).toLocalDate() == date
        }
        val todayApprovals = allExplanations.filter {
            it.approvedAt?.atZone(java.time.ZoneOffset.UTC)?.toLocalDate() == date
        }
        val allVotes = voteRepository.findAll()
        val allFlags = flagRepository.findAll()
        val activeUserIds = activityRepository.findAll()
            .filter { it.activityDate == date }
            .map { it.user.id }
            .distinct()

        val approvedWithScore = allExplanations
            .filter { it.submissionStatus == ExplanationStatus.APPROVED && it.qualityScore != null }
        val avgQuality = if (approvedWithScore.isNotEmpty())
            BigDecimal(approvedWithScore.mapNotNull { it.qualityScore }.average())
                .setScale(2, RoundingMode.HALF_UP)
        else null

        val helpfulVotes = allVotes.count { it.isHelpful }
        val avgHelpful = if (allVotes.isNotEmpty())
            BigDecimal(helpfulVotes.toDouble() / allVotes.size * 100)
                .setScale(2, RoundingMode.HALF_UP)
        else null

        existing.apply {
            totalQuestions      = questionRepository.count().toInt()
            totalBooklets       = bookletRepository.count().toInt()
            totalActs           = actRepository.count().toInt()
            totalExplanations   = allExplanations.size
            pendingExplanations = allExplanations.count { it.submissionStatus == ExplanationStatus.PENDING }
            approvedExplanations= allExplanations.count { it.submissionStatus == ExplanationStatus.APPROVED }
            rejectedExplanations= allExplanations.count { it.submissionStatus == ExplanationStatus.REJECTED }
            flaggedExplanations = allExplanations.count { it.submissionStatus == ExplanationStatus.FLAGGED }
            newExplanationsToday= todayExplanations.size
            newApprovalsToday   = todayApprovals.size
            totalUsers          = userRepository.count().toInt()
            newUsersToday       = userRepository.findAll()
                .count { it.createdAt?.atZone(java.time.ZoneOffset.UTC)?.toLocalDate() == date }
            activeUsersToday    = activeUserIds.size
            totalVotes          = allVotes.size
            totalHelpfulVotes   = helpfulVotes
            totalFlags          = allFlags.size
            openFlags           = allFlags.count { it.flagStatus == FlagStatus.OPEN }
            totalReviews        = reviewRepository.count().toInt()
            avgQualityScore     = avgQuality
            avgHelpfulPct       = avgHelpful
        }

        return snapshotRepository.save(existing)
    }

    @Transactional
    fun buildUserGrowthForDate(date: LocalDate): AnalyticsUserGrowth {
        val existing = userGrowthRepository.findBySnapshotDate(date)
            ?: AnalyticsUserGrowth(snapshotDate = date)

        val allUsers = userRepository.findAll()

        existing.apply {
            totalUsers        = allUsers.size
            publicUsers       = allUsers.count { it.role.name == "PUBLIC_USER" }
            catechists        = allUsers.count { it.role.name == "CATECHIST" }
            priests           = allUsers.count { it.role.name == "PRIEST" }
            theologyReviewers = allUsers.count { it.role.name == "THEOLOGY_REVIEWER" }
            admins            = allUsers.count { it.role.name == "ADMIN" }
            newRegistrations  = allUsers.count {
                it.createdAt?.atZone(java.time.ZoneOffset.UTC)?.toLocalDate() == date
            }
        }

        return userGrowthRepository.save(existing)
    }

    @Transactional
    fun buildModerationPerformanceForDate(date: LocalDate): AnalyticsModerationPerformance {
        val existing = moderationRepository.findBySnapshotDate(date)
            ?: AnalyticsModerationPerformance(snapshotDate = date)

        val allReviews = reviewRepository.findAll()
        val allExplanations = explanationRepository.findAll()
        val allFlags = flagRepository.findAll()

        // Average hours from submission to first review
        val reviewedExplanations = allExplanations.filter { it.reviewedAt != null }
        val avgHours = if (reviewedExplanations.isNotEmpty()) {
            val totalHours = reviewedExplanations.sumOf { exp ->
                java.time.Duration.between(exp.submittedAt, exp.reviewedAt).toHours()
            }
            BigDecimal(totalHours.toDouble() / reviewedExplanations.size)
                .setScale(2, RoundingMode.HALF_UP)
        } else null

        val todayReviews = allReviews.filter {
            it.reviewedAt.atZone(java.time.ZoneOffset.UTC).toLocalDate() == date
        }
        val todayFlagsResolved = allFlags.filter {
            it.resolvedAt?.atZone(java.time.ZoneOffset.UTC)?.toLocalDate() == date
        }

        existing.apply {
            avgReviewHours    = avgHours
            queueLength       = allExplanations.count {
                it.submissionStatus == ExplanationStatus.PENDING ||
                        it.submissionStatus == ExplanationStatus.UNDER_REVIEW
            }
            reviewsCompletedToday = todayReviews.size
            flagsResolvedToday    = todayFlagsResolved.size
        }

        return moderationRepository.save(existing)
    }

    // =====================================================
    // DASHBOARD QUERIES
    // =====================================================

    @Transactional(readOnly = true)
    fun getDashboardSummary(): DashboardSummary {
        val latest = snapshotRepository.findTopByOrderBySnapshotDateDesc()
        val yesterday = snapshotRepository.findBySnapshotDate(LocalDate.now().minusDays(1))
        val latestGrowth = userGrowthRepository.findTopByOrderBySnapshotDateDesc()
        val latestModeration = moderationRepository.findTopByOrderBySnapshotDateDesc()

        return DashboardSummary(
            snapshotDate         = latest?.snapshotDate ?: LocalDate.now(),
            totalQuestions       = latest?.totalQuestions ?: 0,
            totalBooklets        = latest?.totalBooklets ?: 0,
            totalUsers           = latest?.totalUsers ?: 0,
            totalExplanations    = latest?.totalExplanations ?: 0,
            approvedExplanations = latest?.approvedExplanations ?: 0,
            pendingExplanations  = latest?.pendingExplanations ?: 0,
            openFlags            = latest?.openFlags ?: 0,
            avgQualityScore      = latest?.avgQualityScore,
            avgHelpfulPct        = latest?.avgHelpfulPct,

            // Day-over-day deltas
            newExplanationsToday = latest?.newExplanationsToday ?: 0,
            newUsersToday        = latest?.newUsersToday ?: 0,
            newApprovalsToday    = latest?.newApprovalsToday ?: 0,
            activeUsersToday     = latest?.activeUsersToday ?: 0,

            // Moderation
            moderationQueueLength    = latestModeration?.queueLength ?: 0,
            avgReviewHours           = latestModeration?.avgReviewHours,
            reviewsCompletedToday    = latestModeration?.reviewsCompletedToday ?: 0,

            // User roles
            roleBreakdown = latestGrowth?.let {
                RoleBreakdown(
                    publicUsers       = it.publicUsers,
                    catechists        = it.catechists,
                    priests           = it.priests,
                    theologyReviewers = it.theologyReviewers,
                    admins            = it.admins
                )
            }
        )
    }

    @Transactional(readOnly = true)
    fun getExplanationTrend(days: Int = 30): List<AnalyticsDailySnapshot> {
        val from = LocalDate.now().minusDays(days.toLong())
        return snapshotRepository.findFromDate(from)
    }

    @Transactional(readOnly = true)
    fun getUserGrowthTrend(days: Int = 30): List<AnalyticsUserGrowth> {
        val from = LocalDate.now().minusDays(days.toLong())
        return userGrowthRepository.findFromDate(from)
    }

    @Transactional(readOnly = true)
    fun getModerationTrend(days: Int = 30): List<AnalyticsModerationPerformance> {
        val from = LocalDate.now().minusDays(days.toLong())
        return moderationRepository.findFromDate(from)
    }

    @Transactional(readOnly = true)
    fun getTopExplanations(limit: Int = 10): List<TopContentEntry> {
        return explanationRepository
            .findBySubmissionStatus(ExplanationStatus.APPROVED)
            .sortedByDescending { it.qualityScore ?: 0 }
            .take(limit)
            .mapIndexed { index, exp ->
                TopContentEntry(
                    rank        = index + 1,
                    entityId    = exp.id,
                    entityType  = "EXPLANATION",
                    label       = "Q${exp.question.questionNumber} — ${exp.submitter.name}",
                    metricKey   = "quality_score",
                    metricValue = BigDecimal(exp.qualityScore ?: 0)
                )
            }
    }

    @Transactional(readOnly = true)
    fun getContentBreakdown(): ContentBreakdown {
        val allExplanations = explanationRepository.findAll()
        return ContentBreakdown(
            byStatus = allExplanations
                .groupingBy { it.submissionStatus.name }
                .eachCount()
                .map { ContentBreakdownEntry(it.key, it.value) }
                .sortedByDescending { it.count },
            byType = allExplanations
                .groupingBy { it.contentType.name }
                .eachCount()
                .map { ContentBreakdownEntry(it.key, it.value) }
                .sortedByDescending { it.count },
            byLanguage = allExplanations
                .groupingBy { it.languageCode }
                .eachCount()
                .map { ContentBreakdownEntry(it.key, it.value) }
                .sortedByDescending { it.count }
        )
    }

    /**
     * Trigger a snapshot manually (e.g. from admin UI).
     */
    @Transactional
    fun triggerSnapshot(): DashboardSummary {
        buildDailySnapshot()
        return getDashboardSummary()
    }
}

// =====================================================
// RESULT TYPES
// =====================================================

data class DashboardSummary(
    val snapshotDate: LocalDate,
    val totalQuestions: Int,
    val totalBooklets: Int,
    val totalUsers: Int,
    val totalExplanations: Int,
    val approvedExplanations: Int,
    val pendingExplanations: Int,
    val openFlags: Int,
    val avgQualityScore: BigDecimal?,
    val avgHelpfulPct: BigDecimal?,
    val newExplanationsToday: Int,
    val newUsersToday: Int,
    val newApprovalsToday: Int,
    val activeUsersToday: Int,
    val moderationQueueLength: Int,
    val avgReviewHours: BigDecimal?,
    val reviewsCompletedToday: Int,
    val roleBreakdown: RoleBreakdown?
)

data class RoleBreakdown(
    val publicUsers: Int,
    val catechists: Int,
    val priests: Int,
    val theologyReviewers: Int,
    val admins: Int
)

data class TopContentEntry(
    val rank: Int,
    val entityId: java.util.UUID,
    val entityType: String,
    val label: String,
    val metricKey: String,
    val metricValue: BigDecimal
)

data class ContentBreakdown(
    val byStatus: List<ContentBreakdownEntry>,
    val byType: List<ContentBreakdownEntry>,
    val byLanguage: List<ContentBreakdownEntry>
)

data class ContentBreakdownEntry(
    val label: String,
    val count: Int
)