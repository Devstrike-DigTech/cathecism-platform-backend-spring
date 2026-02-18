-- V4__Add_explanation_system.sql
-- Phase 2: Explanation Submission and Moderation System

-- =====================================================
-- EXPLANATION TYPES AND STATUS
-- =====================================================

-- Explanation submission (main table)
CREATE TABLE explanation_submission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES catechism_question(id) ON DELETE CASCADE,
    submitter_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    content_type VARCHAR(20) NOT NULL CHECK (content_type IN ('TEXT', 'AUDIO', 'VIDEO')),

    -- Text content (for TEXT type)
    text_content TEXT,

    -- File metadata (for AUDIO/VIDEO type)
    file_url TEXT,
    file_size_bytes BIGINT,
    file_mime_type VARCHAR(100),
    duration_seconds INTEGER,

    -- Submission metadata
    submission_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (submission_status IN ('PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'FLAGGED')),
    quality_score INTEGER CHECK (quality_score >= 0 AND quality_score <= 100),
    view_count INTEGER DEFAULT 0,
    helpful_count INTEGER DEFAULT 0,

    -- Timestamps
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    approved_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Moderation reviews
CREATE TABLE explanation_review (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    explanation_id UUID NOT NULL REFERENCES explanation_submission(id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,

    -- Review details
    review_status VARCHAR(20) NOT NULL CHECK (review_status IN ('APPROVED', 'REJECTED', 'NEEDS_REVISION')),
    review_comments TEXT,
    quality_rating INTEGER CHECK (quality_rating >= 1 AND quality_rating <= 5),

    -- Review criteria scores (1-5)
    accuracy_score INTEGER CHECK (accuracy_score >= 1 AND accuracy_score <= 5),
    clarity_score INTEGER CHECK (clarity_score >= 1 AND clarity_score <= 5),
    theological_soundness_score INTEGER CHECK (theological_soundness_score >= 1 AND theological_soundness_score <= 5),

    reviewed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Explanation flags (for community reporting)
CREATE TABLE explanation_flag (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    explanation_id UUID NOT NULL REFERENCES explanation_submission(id) ON DELETE CASCADE,
    flagger_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,

    flag_reason VARCHAR(50) NOT NULL CHECK (flag_reason IN (
        'INACCURATE',
        'INAPPROPRIATE',
        'MISLEADING',
        'POOR_QUALITY',
        'DUPLICATE',
        'OFF_TOPIC',
        'OTHER'
    )),
    flag_details TEXT,
    flag_status VARCHAR(20) DEFAULT 'OPEN' CHECK (flag_status IN ('OPEN', 'REVIEWED', 'RESOLVED', 'DISMISSED')),

    -- Moderator response
    moderator_id UUID REFERENCES app_user(id),
    moderator_notes TEXT,
    resolved_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- User votes/ratings on explanations
CREATE TABLE explanation_vote (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    explanation_id UUID NOT NULL REFERENCES explanation_submission(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,

    is_helpful BOOLEAN NOT NULL,
    vote_comment TEXT,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- One vote per user per explanation
    UNIQUE(explanation_id, user_id)
);

-- File uploads metadata (for tracking all uploaded files)
CREATE TABLE file_upload (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uploader_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,

    file_name VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,

    -- File type categorization
    upload_type VARCHAR(20) NOT NULL CHECK (upload_type IN ('AUDIO', 'VIDEO', 'IMAGE', 'DOCUMENT')),

    -- Processing status (for video/audio transcoding)
    processing_status VARCHAR(20) DEFAULT 'PENDING'
        CHECK (processing_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),

    -- Security
    virus_scan_status VARCHAR(20) DEFAULT 'PENDING'
        CHECK (virus_scan_status IN ('PENDING', 'CLEAN', 'INFECTED', 'FAILED')),

    -- Usage tracking
    is_public BOOLEAN DEFAULT false,
    access_count INTEGER DEFAULT 0,

    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

-- Explanation submissions
CREATE INDEX idx_explanation_question ON explanation_submission(question_id);
CREATE INDEX idx_explanation_submitter ON explanation_submission(submitter_id);
CREATE INDEX idx_explanation_status ON explanation_submission(submission_status);
CREATE INDEX idx_explanation_language ON explanation_submission(language_code);
CREATE INDEX idx_explanation_submitted_at ON explanation_submission(submitted_at DESC);

-- Reviews
CREATE INDEX idx_review_explanation ON explanation_review(explanation_id);
CREATE INDEX idx_review_reviewer ON explanation_review(reviewer_id);
CREATE INDEX idx_review_reviewed_at ON explanation_review(reviewed_at DESC);

-- Flags
CREATE INDEX idx_flag_explanation ON explanation_flag(explanation_id);
CREATE INDEX idx_flag_flagger ON explanation_flag(flagger_id);
CREATE INDEX idx_flag_status ON explanation_flag(flag_status);
CREATE INDEX idx_flag_moderator ON explanation_flag(moderator_id);

-- Votes
CREATE INDEX idx_vote_explanation ON explanation_vote(explanation_id);
CREATE INDEX idx_vote_user ON explanation_vote(user_id);

-- File uploads
CREATE INDEX idx_file_uploader ON file_upload(uploader_id);
CREATE INDEX idx_file_type ON file_upload(upload_type);
CREATE INDEX idx_file_processing_status ON file_upload(processing_status);
CREATE INDEX idx_file_uploaded_at ON file_upload(uploaded_at DESC);

-- =====================================================
-- VIEWS FOR COMMON QUERIES
-- =====================================================

-- View for explanation with review summary
CREATE VIEW v_explanation_with_review AS
SELECT
    e.*,
    u.name as submitter_name,
    u.email as submitter_email,
    COUNT(DISTINCT r.id) as review_count,
    AVG(r.quality_rating) as avg_quality_rating,
    COUNT(DISTINCT CASE WHEN v.is_helpful = true THEN v.id END) as helpful_votes,
    COUNT(DISTINCT CASE WHEN v.is_helpful = false THEN v.id END) as unhelpful_votes,
    COUNT(DISTINCT f.id) as flag_count
FROM explanation_submission e
LEFT JOIN app_user u ON e.submitter_id = u.id
LEFT JOIN explanation_review r ON e.id = r.explanation_id
LEFT JOIN explanation_vote v ON e.id = v.explanation_id
LEFT JOIN explanation_flag f ON e.id = f.explanation_id AND f.flag_status = 'OPEN'
GROUP BY e.id, u.name, u.email;

-- View for moderation queue (pending items)
CREATE VIEW v_moderation_queue AS
SELECT
    e.*,
    u.name as submitter_name,
    q.question_number,
    qt.question_text,
    COUNT(DISTINCT f.id) as flag_count,
    MAX(f.created_at) as latest_flag_at
FROM explanation_submission e
JOIN app_user u ON e.submitter_id = u.id
JOIN catechism_question q ON e.question_id = q.id
LEFT JOIN catechism_question_translation qt ON q.id = qt.question_id AND qt.language_code = e.language_code
LEFT JOIN explanation_flag f ON e.id = f.explanation_id AND f.flag_status = 'OPEN'
WHERE e.submission_status IN ('PENDING', 'FLAGGED', 'UNDER_REVIEW')
GROUP BY e.id, u.name, q.question_number, qt.question_text
ORDER BY
    CASE e.submission_status
        WHEN 'FLAGGED' THEN 1
        WHEN 'UNDER_REVIEW' THEN 2
        WHEN 'PENDING' THEN 3
    END,
    e.submitted_at ASC;

-- =====================================================
-- COMMENTS
-- =====================================================

COMMENT ON TABLE explanation_submission IS 'User-submitted explanations for catechism questions';
COMMENT ON TABLE explanation_review IS 'Moderator reviews of submitted explanations';
COMMENT ON TABLE explanation_flag IS 'Community flags for problematic explanations';
COMMENT ON TABLE explanation_vote IS 'User votes on explanation helpfulness';
COMMENT ON TABLE file_upload IS 'Metadata for all uploaded audio/video/document files';

COMMENT ON COLUMN explanation_submission.content_type IS 'Type: TEXT, AUDIO, or VIDEO';
COMMENT ON COLUMN explanation_submission.submission_status IS 'Status: PENDING, UNDER_REVIEW, APPROVED, REJECTED, FLAGGED';
COMMENT ON COLUMN explanation_submission.quality_score IS 'Calculated score 0-100 based on reviews and votes';