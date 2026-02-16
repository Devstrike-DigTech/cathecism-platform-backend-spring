-- V1__Initial_schema.sql
-- Phase 1: Foundation tables for Catechism Platform

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- BOOKLET AND QUESTIONS
-- =====================================================

CREATE TABLE catechism_booklet (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    diocese VARCHAR(255),
    version VARCHAR(50) NOT NULL,
    language_default VARCHAR(10) NOT NULL DEFAULT 'en',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_booklet_diocese ON catechism_booklet(diocese);

CREATE TABLE catechism_question (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booklet_id UUID NOT NULL REFERENCES catechism_booklet(id) ON DELETE CASCADE,
    question_number INT NOT NULL,
    category VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(booklet_id, question_number)
);

CREATE INDEX idx_question_booklet ON catechism_question(booklet_id);
CREATE INDEX idx_question_category ON catechism_question(category);

CREATE TABLE catechism_question_translation (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    question_id UUID NOT NULL REFERENCES catechism_question(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    question_text TEXT NOT NULL,
    answer_text TEXT NOT NULL,
    is_official BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(question_id, language_code)
);

CREATE INDEX idx_question_translation_lang ON catechism_question_translation(language_code);
CREATE INDEX idx_question_translation_question ON catechism_question_translation(question_id);

-- =====================================================
-- CATECHISM OF THE CATHOLIC CHURCH (CCC)
-- =====================================================

CREATE TABLE ccc_paragraph (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    paragraph_number INT NOT NULL UNIQUE,
    edition VARCHAR(50) NOT NULL DEFAULT '2nd Edition',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ccc_paragraph_number ON ccc_paragraph(paragraph_number);

CREATE TABLE ccc_paragraph_translation (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ccc_id UUID NOT NULL REFERENCES ccc_paragraph(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    paragraph_text TEXT NOT NULL,
    licensed BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(ccc_id, language_code)
);

CREATE INDEX idx_ccc_translation_lang ON ccc_paragraph_translation(language_code);
CREATE INDEX idx_ccc_translation_ccc ON ccc_paragraph_translation(ccc_id);

-- =====================================================
-- BIBLE REFERENCES
-- =====================================================

CREATE TABLE bible_reference (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    book VARCHAR(50) NOT NULL,
    chapter INT NOT NULL,
    verse_start INT NOT NULL,
    verse_end INT,
    translation VARCHAR(50) NOT NULL DEFAULT 'RSV-CE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(book, chapter, verse_start, verse_end, translation)
);

CREATE INDEX idx_bible_book ON bible_reference(book);
CREATE INDEX idx_bible_chapter ON bible_reference(book, chapter);

CREATE TABLE bible_reference_translation (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    bible_reference_id UUID NOT NULL REFERENCES bible_reference(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    verse_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(bible_reference_id, language_code)
);

CREATE INDEX idx_bible_translation_lang ON bible_reference_translation(language_code);
CREATE INDEX idx_bible_translation_ref ON bible_reference_translation(bible_reference_id);

-- =====================================================
-- JUNCTION TABLES (Many-to-Many)
-- =====================================================

CREATE TABLE question_ccc_reference (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    question_id UUID NOT NULL REFERENCES catechism_question(id) ON DELETE CASCADE,
    ccc_id UUID NOT NULL REFERENCES ccc_paragraph(id) ON DELETE CASCADE,
    reference_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(question_id, ccc_id)
);

CREATE INDEX idx_qccc_question ON question_ccc_reference(question_id);
CREATE INDEX idx_qccc_ccc ON question_ccc_reference(ccc_id);

CREATE TABLE question_bible_reference (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    question_id UUID NOT NULL REFERENCES catechism_question(id) ON DELETE CASCADE,
    bible_reference_id UUID NOT NULL REFERENCES bible_reference(id) ON DELETE CASCADE,
    reference_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(question_id, bible_reference_id)
);

CREATE INDEX idx_qbible_question ON question_bible_reference(question_id);
CREATE INDEX idx_qbible_bible ON question_bible_reference(bible_reference_id);

-- =====================================================
-- USERS AND AUTHENTICATION
-- =====================================================

CREATE TABLE app_user (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'PUBLIC_USER',
    verified BOOLEAN NOT NULL DEFAULT false,
    verification_notes TEXT,
    diocese VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_email ON app_user(email);
CREATE INDEX idx_user_role ON app_user(role);

-- Add constraint to ensure valid roles
ALTER TABLE app_user ADD CONSTRAINT check_user_role 
    CHECK (role IN ('PUBLIC_USER', 'CATECHIST', 'PRIEST', 'THEOLOGY_REVIEWER', 'ADMIN'));

-- =====================================================
-- SEED DATA (Optional - for development)
-- =====================================================

-- Create default admin user (password: admin123)
INSERT INTO app_user (email, password_hash, name, role, verified)
VALUES (
    'admin@catechism.com',
    '$2a$10$xqyZwG8X7Y.Y9qKQvw8bOu8l7UJKM1Bf7jXWzJ.UG2U/hN3OXLZ6m',  -- bcrypt hash of "admin123"
    'System Administrator',
    'ADMIN',
    true
);
