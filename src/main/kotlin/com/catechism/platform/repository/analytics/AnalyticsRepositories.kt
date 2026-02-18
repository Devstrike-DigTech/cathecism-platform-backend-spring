package com.catechism.platform.repository.analytics

import com.catechism.platform.domain.analytics.*
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

// =====================================================
// ANALYTICS DAILY SNAPSHOT
// =====================================================

@Repository
interface AnalyticsDailySnapshotRepository : JpaRepository<AnalyticsDailySnapshot, UUID> {

    /**
     * Find the snapshot for a specific calendar date.
     * Returns null if no snapshot has been generated for that date yet.
     */
    fun findBySnapshotDate(date: LocalDate): AnalyticsDailySnapshot?

    /**
     * Find the most recent snapshot — used by dashboardSummary to get the latest data.
     */
    fun findTopByOrderBySnapshotDateDesc(): AnalyticsDailySnapshot?

    /**
     * Find all snapshots from a given date onwards, ordered oldest-first.
     * Used to build time-series data for trend charts.
     * Example: findFromDate(LocalDate.now().minusDays(30))
     */
    @Query("SELECT s FROM AnalyticsDailySnapshot s WHERE s.snapshotDate >= :from ORDER BY s.snapshotDate ASC")
    fun findFromDate(@Param("from") from: LocalDate): List<AnalyticsDailySnapshot>

    /**
     * Find the N most recent snapshots, newest first.
     * Pass PageRequest.of(0, n) as the pageable argument.
     */
    @Query("SELECT s FROM AnalyticsDailySnapshot s ORDER BY s.snapshotDate DESC")
    fun findRecentSnapshots(pageable: Pageable): List<AnalyticsDailySnapshot>
}

// =====================================================
// ANALYTICS USER GROWTH
// =====================================================

@Repository
interface AnalyticsUserGrowthRepository : JpaRepository<AnalyticsUserGrowth, UUID> {

    /**
     * Find the user-growth snapshot for a specific calendar date.
     * Returns null if no snapshot exists for that date.
     */
    fun findBySnapshotDate(date: LocalDate): AnalyticsUserGrowth?

    /**
     * Find the most recent user-growth snapshot.
     */
    fun findTopByOrderBySnapshotDateDesc(): AnalyticsUserGrowth?

    /**
     * Find all user-growth snapshots from a given date onwards, ordered oldest-first.
     * Used to build the user growth trend chart.
     */
    @Query("SELECT g FROM AnalyticsUserGrowth g WHERE g.snapshotDate >= :from ORDER BY g.snapshotDate ASC")
    fun findFromDate(@Param("from") from: LocalDate): List<AnalyticsUserGrowth>
}

// =====================================================
// ANALYTICS MODERATION PERFORMANCE
// =====================================================

@Repository
interface AnalyticsModerationPerformanceRepository : JpaRepository<AnalyticsModerationPerformance, UUID> {

    /**
     * Find the moderation-performance snapshot for a specific calendar date.
     * Returns null if no snapshot exists for that date.
     */
    fun findBySnapshotDate(date: LocalDate): AnalyticsModerationPerformance?

    /**
     * Find the most recent moderation-performance snapshot.
     */
    fun findTopByOrderBySnapshotDateDesc(): AnalyticsModerationPerformance?

    /**
     * Find all moderation-performance snapshots from a given date onwards, ordered oldest-first.
     * Used to build the moderation trend chart.
     */
    @Query("SELECT m FROM AnalyticsModerationPerformance m WHERE m.snapshotDate >= :from ORDER BY m.snapshotDate ASC")
    fun findFromDate(@Param("from") from: LocalDate): List<AnalyticsModerationPerformance>
}