-- V6__Add_analytics.sql
-- Phase 4: Analytics Dashboard

-- =====================================================
-- DAILY SNAPSHOT TABLE
-- Stores pre-aggregated daily stats for fast dashboard queries.
-- Populated nightly by the analytics job.
-- =====================================================

CREATE TABLE analytics_daily_snapshot (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    snapshot_date   DATE NOT NULL UNIQUE,

    -- Content
    total_questions         INT NOT NULL DEFAULT 0,
    total_booklets          INT NOT NULL DEFAULT 0,
    total_acts              INT NOT NULL DEFAULT 0,

    -- Explanations
    total_explanations      INT NOT NULL DEFAULT 0,
    pending_explanations    INT NOT NULL DEFAULT 0,
    approved_explanations   INT NOT NULL DEFAULT 0,
    rejected_explanations   INT NOT NULL DEFAULT 0,
    flagged_explanations    INT NOT NULL DEFAULT 0,
    new_explanations_today  INT NOT NULL DEFAULT 0,
    new_approvals_today     INT NOT NULL DEFAULT 0,

    -- Users
    total_users             INT NOT NULL DEFAULT 0,
    new_users_today         INT NOT NULL DEFAULT 0,
    active_users_today      INT NOT NULL DEFAULT 0,   -- users with any activity today

    -- Engagement
    total_votes             INT NOT NULL DEFAULT 0,
    total_helpful_votes     INT NOT NULL DEFAULT 0,
    total_flags             INT NOT NULL DEFAULT 0,
    open_flags              INT NOT NULL DEFAULT 0,
    total_reviews           INT NOT NULL DEFAULT 0,

    -- Quality
    avg_quality_score       NUMERIC(5,2),
    avg_helpful_pct         NUMERIC(5,2),

    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =====================================================
-- EXPLANATION DAILY ACTIVITY
-- Per-day breakdown of explanation workflow activity.
-- =====================================================

CREATE TABLE analytics_explanation_activity (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    activity_date   DATE NOT NULL,
    booklet_id      UUID REFERENCES catechism_booklet(id) ON DELETE SET NULL,
    language_code   VARCHAR(10) NOT NULL DEFAULT 'en',
    content_type    VARCHAR(10) NOT NULL,   -- TEXT, AUDIO, VIDEO

    submitted_count INT NOT NULL DEFAULT 0,
    approved_count  INT NOT NULL DEFAULT 0,
    rejected_count  INT NOT NULL DEFAULT 0,
    flagged_count   INT NOT NULL DEFAULT 0,
    votes_count     INT NOT NULL DEFAULT 0,

    UNIQUE (activity_date, booklet_id, language_code, content_type)
);

-- =====================================================
-- USER GROWTH
-- Daily user registration and role breakdown.
-- =====================================================

CREATE TABLE analytics_user_growth (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    snapshot_date   DATE NOT NULL UNIQUE,
    total_users     INT NOT NULL DEFAULT 0,
    public_users    INT NOT NULL DEFAULT 0,
    catechists      INT NOT NULL DEFAULT 0,
    priests         INT NOT NULL DEFAULT 0,
    theology_reviewers INT NOT NULL DEFAULT 0,
    admins          INT NOT NULL DEFAULT 0,
    new_registrations INT NOT NULL DEFAULT 0
);

-- =====================================================
-- TOP CONTENT STATS
-- Stores daily rankings of most-viewed / highest-quality content.
-- =====================================================

CREATE TABLE analytics_top_content (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    snapshot_date   DATE NOT NULL,
    content_type    VARCHAR(20) NOT NULL,   -- QUESTION, EXPLANATION
    entity_id       UUID NOT NULL,
    rank            INT NOT NULL,
    metric_value    NUMERIC(10,2) NOT NULL,
    metric_key      VARCHAR(50) NOT NULL,   -- view_count, quality_score, helpful_votes

    UNIQUE (snapshot_date, content_type, metric_key, rank)
);

-- =====================================================
-- MODERATION PERFORMANCE
-- Tracks how long explanations spend in moderation.
-- =====================================================

CREATE TABLE analytics_moderation_performance (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    snapshot_date           DATE NOT NULL UNIQUE,
    avg_review_hours        NUMERIC(8,2),   -- avg hours from PENDING → reviewed
    median_review_hours     NUMERIC(8,2),
    queue_length            INT NOT NULL DEFAULT 0,
    reviews_completed_today INT NOT NULL DEFAULT 0,
    flags_resolved_today    INT NOT NULL DEFAULT 0
);

-- =====================================================
-- INDEXES
-- =====================================================

CREATE INDEX idx_analytics_snapshot_date   ON analytics_daily_snapshot(snapshot_date DESC);
CREATE INDEX idx_analytics_activity_date   ON analytics_explanation_activity(activity_date DESC);
CREATE INDEX idx_analytics_activity_booklet ON analytics_explanation_activity(booklet_id, activity_date DESC);
CREATE INDEX idx_analytics_user_growth     ON analytics_user_growth(snapshot_date DESC);
CREATE INDEX idx_analytics_top_content     ON analytics_top_content(snapshot_date DESC, content_type, metric_key);
CREATE INDEX idx_analytics_moderation      ON analytics_moderation_performance(snapshot_date DESC);