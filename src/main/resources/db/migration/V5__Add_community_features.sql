-- V5__Add_community_features.sql
-- Phase 3: Community Features - Profiles, Badges, Achievements, Leaderboards

-- =====================================================
-- USER PROFILES
-- =====================================================

CREATE TABLE user_profile (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
    bio             TEXT,
    avatar_url      TEXT,
    location        TEXT,
    website_url     TEXT,
    display_name    VARCHAR(100),
    is_public       BOOLEAN NOT NULL DEFAULT true,
    -- Contribution stats (denormalised for performance)
    total_submissions   INT NOT NULL DEFAULT 0,
    approved_submissions INT NOT NULL DEFAULT 0,
    total_votes_cast    INT NOT NULL DEFAULT 0,
    total_helpful_votes INT NOT NULL DEFAULT 0,   -- votes received on their submissions
    total_flags_resolved INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =====================================================
-- BADGE DEFINITIONS
-- =====================================================

CREATE TABLE badge (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(50) NOT NULL UNIQUE,  -- e.g. 'FIRST_SUBMISSION'
    name            VARCHAR(100) NOT NULL,
    description     TEXT NOT NULL,
    icon_url        TEXT,
    badge_category  VARCHAR(50) NOT NULL,          -- SUBMISSION, REVIEW, COMMUNITY, SPECIAL
    points_value    INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =====================================================
-- USER BADGES (earned)
-- =====================================================

CREATE TABLE user_badge (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    badge_id        UUID NOT NULL REFERENCES badge(id) ON DELETE CASCADE,
    earned_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    context_note    TEXT,   -- e.g. "For submitting your 10th explanation"
    UNIQUE (user_id, badge_id)
);

-- =====================================================
-- ACHIEVEMENT DEFINITIONS
-- =====================================================

CREATE TABLE achievement (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(50) NOT NULL UNIQUE,
    name                VARCHAR(100) NOT NULL,
    description         TEXT NOT NULL,
    icon_url            TEXT,
    achievement_category VARCHAR(50) NOT NULL,
    -- Threshold-based: award when metric reaches target_value
    metric_key          VARCHAR(100) NOT NULL,  -- e.g. 'approved_submissions'
    target_value        INT NOT NULL,
    points_value        INT NOT NULL DEFAULT 0,
    badge_id            UUID REFERENCES badge(id),  -- optional badge awarded alongside
    is_active           BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =====================================================
-- USER ACHIEVEMENTS (progress + completion)
-- =====================================================

CREATE TABLE user_achievement (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    achievement_id  UUID NOT NULL REFERENCES achievement(id) ON DELETE CASCADE,
    current_value   INT NOT NULL DEFAULT 0,
    completed       BOOLEAN NOT NULL DEFAULT false,
    completed_at    TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, achievement_id)
);

-- =====================================================
-- CONTRIBUTION ACTIVITY LOG
-- =====================================================

CREATE TABLE contribution_activity (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    activity_type   VARCHAR(50) NOT NULL,  -- SUBMISSION, REVIEW, VOTE, FLAG_RESOLVED
    entity_type     VARCHAR(50) NOT NULL,  -- EXPLANATION, FLAG, VOTE
    entity_id       UUID NOT NULL,
    points_earned   INT NOT NULL DEFAULT 0,
    activity_date   DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =====================================================
-- LEADERBOARD SNAPSHOTS (weekly / all-time)
-- =====================================================

CREATE TABLE leaderboard_entry (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    leaderboard_type VARCHAR(20) NOT NULL,  -- WEEKLY, MONTHLY, ALL_TIME
    period_key      VARCHAR(20) NOT NULL,   -- e.g. '2026-W07', '2026-02', 'ALL'
    rank            INT NOT NULL,
    total_points    INT NOT NULL DEFAULT 0,
    submissions     INT NOT NULL DEFAULT 0,
    approvals       INT NOT NULL DEFAULT 0,
    helpful_votes   INT NOT NULL DEFAULT 0,
    snapshot_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, leaderboard_type, period_key)
);

-- =====================================================
-- SEED: BADGE DEFINITIONS
-- =====================================================

INSERT INTO badge (code, name, description, badge_category, points_value) VALUES
-- Submission badges
('FIRST_SUBMISSION',    'First Steps',          'Submitted your first explanation',             'SUBMISSION', 10),
('SUBMISSION_5',        'Getting Started',      'Submitted 5 explanations',                     'SUBMISSION', 25),
('SUBMISSION_10',       'Contributor',          'Submitted 10 explanations',                    'SUBMISSION', 50),
('SUBMISSION_50',       'Prolific Writer',      'Submitted 50 explanations',                    'SUBMISSION', 150),
('SUBMISSION_100',      'Catechism Champion',   'Submitted 100 explanations',                   'SUBMISSION', 300),
-- Quality badges
('FIRST_APPROVAL',      'Approved!',            'Had your first explanation approved',           'SUBMISSION', 15),
('APPROVAL_10',         'Trusted Voice',        '10 approved explanations',                     'SUBMISSION', 75),
('APPROVAL_50',         'Expert Contributor',   '50 approved explanations',                     'SUBMISSION', 200),
('PERFECT_SCORE',       'Perfect Score',        'Received a quality score of 100',              'SUBMISSION', 100),
-- Community badges
('FIRST_VOTE',          'Cast Your Vote',       'Voted on your first explanation',              'COMMUNITY', 5),
('VOTE_50',             'Active Voter',         'Voted on 50 explanations',                     'COMMUNITY', 30),
('VOTE_200',            'Community Pillar',     'Voted on 200 explanations',                    'COMMUNITY', 75),
('HELPFUL_10',          'Helpful Explanations', 'Received 10 helpful votes',                    'COMMUNITY', 40),
('HELPFUL_100',         'Community Favourite',  'Received 100 helpful votes',                   'COMMUNITY', 150),
-- Review badges (for moderators)
('FIRST_REVIEW',        'First Review',         'Completed your first moderation review',       'REVIEW', 20),
('REVIEW_25',           'Diligent Reviewer',    'Completed 25 reviews',                         'REVIEW', 80),
('REVIEW_100',          'Master Reviewer',      'Completed 100 reviews',                        'REVIEW', 250),
-- Special
('MULTILINGUAL',        'Multilingual',         'Submitted explanations in 3+ languages',       'SPECIAL', 100),
('EARLY_ADOPTER',       'Early Adopter',        'One of the first 100 users on the platform',   'SPECIAL', 50);

-- =====================================================
-- SEED: ACHIEVEMENT DEFINITIONS
-- =====================================================

INSERT INTO achievement (code, name, description, achievement_category, metric_key, target_value, points_value) VALUES
('ACH_SUB_1',    'First Steps',          'Submit your first explanation',        'SUBMISSION', 'total_submissions',    1,   10),
('ACH_SUB_5',    'Getting Started',      'Submit 5 explanations',                'SUBMISSION', 'total_submissions',    5,   25),
('ACH_SUB_10',   'Contributor',          'Submit 10 explanations',               'SUBMISSION', 'total_submissions',    10,  50),
('ACH_SUB_50',   'Prolific Writer',      'Submit 50 explanations',               'SUBMISSION', 'total_submissions',    50,  150),
('ACH_APP_1',    'First Approval',       'Get your first explanation approved',  'SUBMISSION', 'approved_submissions', 1,   15),
('ACH_APP_10',   'Trusted Voice',        'Get 10 explanations approved',         'SUBMISSION', 'approved_submissions', 10,  75),
('ACH_APP_50',   'Expert Contributor',   'Get 50 explanations approved',         'SUBMISSION', 'approved_submissions', 50,  200),
('ACH_VOTE_1',   'Cast Your Vote',       'Vote on your first explanation',       'COMMUNITY',  'total_votes_cast',     1,   5),
('ACH_VOTE_50',  'Active Voter',         'Vote on 50 explanations',              'COMMUNITY',  'total_votes_cast',     50,  30),
('ACH_VOTE_200', 'Community Pillar',     'Vote on 200 explanations',             'COMMUNITY',  'total_votes_cast',     200, 75),
('ACH_HLP_10',   'Helpful Voice',        'Receive 10 helpful votes',             'COMMUNITY',  'total_helpful_votes',  10,  40),
('ACH_HLP_100',  'Community Favourite',  'Receive 100 helpful votes',            'COMMUNITY',  'total_helpful_votes',  100, 150),
('ACH_REV_1',    'First Review',         'Complete your first review',           'REVIEW',     'reviews_completed',    1,   20),
('ACH_REV_25',   'Diligent Reviewer',    'Complete 25 reviews',                  'REVIEW',     'reviews_completed',    25,  80),
('ACH_REV_100',  'Master Reviewer',      'Complete 100 reviews',                 'REVIEW',     'reviews_completed',    100, 250);

-- =====================================================
-- INDEXES
-- =====================================================

CREATE INDEX idx_user_profile_user_id     ON user_profile(user_id);
CREATE INDEX idx_user_badge_user_id       ON user_badge(user_id);
CREATE INDEX idx_user_badge_badge_id      ON user_badge(badge_id);
CREATE INDEX idx_user_achievement_user_id ON user_achievement(user_id);
CREATE INDEX idx_user_achievement_completed ON user_achievement(user_id, completed);
CREATE INDEX idx_contribution_user_date   ON contribution_activity(user_id, activity_date);
CREATE INDEX idx_contribution_type        ON contribution_activity(activity_type);
CREATE INDEX idx_leaderboard_type_period  ON leaderboard_entry(leaderboard_type, period_key, rank);
CREATE INDEX idx_leaderboard_user         ON leaderboard_entry(user_id);

-- =====================================================
-- VIEW: USER STATS SUMMARY
-- =====================================================

CREATE VIEW v_user_stats AS
SELECT
    u.id              AS user_id,
    u.name,
    u.role,
    p.display_name,
    p.avatar_url,
    p.bio,
    p.is_public,
    p.total_submissions,
    p.approved_submissions,
    p.total_votes_cast,
    p.total_helpful_votes,
    COALESCE((
        SELECT SUM(a.points_value)
        FROM user_achievement ua
        JOIN achievement a ON ua.achievement_id = a.id
        WHERE ua.user_id = u.id AND ua.completed = true
    ), 0) AS total_points,
    (
        SELECT COUNT(*) FROM user_badge ub WHERE ub.user_id = u.id
    ) AS badge_count
FROM app_user u
LEFT JOIN user_profile p ON p.user_id = u.id;