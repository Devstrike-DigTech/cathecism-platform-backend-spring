-- V2__Add_acts_categorization.sql
-- Adds hierarchical Acts categorization with titles and subtitles

-- =====================================================
-- ACTS STRUCTURE (Hierarchical Categorization)
-- =====================================================

-- Acts table - Top level categorization
-- Example: Act 1: Creed, Act 2: Sacraments, Act 3: Morality
CREATE TABLE catechism_act (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booklet_id UUID NOT NULL REFERENCES catechism_booklet(id) ON DELETE CASCADE,
    act_number INT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(booklet_id, act_number)
);

CREATE INDEX idx_act_booklet ON catechism_act(booklet_id);
CREATE INDEX idx_act_number ON catechism_act(act_number);
CREATE INDEX idx_act_display_order ON catechism_act(booklet_id, display_order);

-- Acts translations (multilingual titles)
CREATE TABLE catechism_act_translation (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    act_id UUID NOT NULL REFERENCES catechism_act(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(act_id, language_code)
);

CREATE INDEX idx_act_translation_lang ON catechism_act_translation(language_code);
CREATE INDEX idx_act_translation_act ON catechism_act_translation(act_id);

-- Act Subtitles (Sub-categorization within an Act)
-- Example: Act 1 (Creed) → Subtitle 1: God the Father, Subtitle 2: Jesus Christ
CREATE TABLE catechism_act_subtitle (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    act_id UUID NOT NULL REFERENCES catechism_act(id) ON DELETE CASCADE,
    subtitle_number INT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(act_id, subtitle_number)
);

CREATE INDEX idx_subtitle_act ON catechism_act_subtitle(act_id);
CREATE INDEX idx_subtitle_number ON catechism_act_subtitle(subtitle_number);
CREATE INDEX idx_subtitle_display_order ON catechism_act_subtitle(act_id, display_order);

-- Act Subtitle translations
CREATE TABLE catechism_act_subtitle_translation (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    subtitle_id UUID NOT NULL REFERENCES catechism_act_subtitle(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(subtitle_id, language_code)
);

CREATE INDEX idx_subtitle_translation_lang ON catechism_act_subtitle_translation(language_code);
CREATE INDEX idx_subtitle_translation_subtitle ON catechism_act_subtitle_translation(subtitle_id);

-- =====================================================
-- UPDATE QUESTIONS TO LINK TO SUBTITLES
-- =====================================================

-- Add subtitle reference to questions
ALTER TABLE catechism_question 
ADD COLUMN subtitle_id UUID REFERENCES catechism_act_subtitle(id) ON DELETE SET NULL;

CREATE INDEX idx_question_subtitle ON catechism_question(subtitle_id);

-- Keep the old category field for backward compatibility or remove if not needed
-- For now we'll keep it as it might be useful for additional tagging

-- =====================================================
-- VIEWS FOR EASY QUERYING
-- =====================================================

-- View to get full question hierarchy
CREATE OR REPLACE VIEW v_question_hierarchy AS
SELECT 
    q.id as question_id,
    q.question_number,
    b.id as booklet_id,
    b.name as booklet_name,
    a.id as act_id,
    a.act_number,
    s.id as subtitle_id,
    s.subtitle_number,
    s.display_order as subtitle_display_order
FROM catechism_question q
JOIN catechism_booklet b ON q.booklet_id = b.id
LEFT JOIN catechism_act_subtitle s ON q.subtitle_id = s.id
LEFT JOIN catechism_act a ON s.act_id = a.id;

-- =====================================================
-- SEED DATA (Example structure)
-- =====================================================

-- Example: Create Acts for a sample booklet
-- Uncomment and modify as needed for your actual booklet

/*
-- Get the first booklet (or create one if needed)
DO $$
DECLARE
    v_booklet_id UUID;
    v_act1_id UUID;
    v_act2_id UUID;
    v_subtitle1_id UUID;
BEGIN
    -- Get or create a booklet
    SELECT id INTO v_booklet_id FROM catechism_booklet LIMIT 1;
    
    IF v_booklet_id IS NOT NULL THEN
        -- Create Act 1: Creed
        INSERT INTO catechism_act (booklet_id, act_number, display_order)
        VALUES (v_booklet_id, 1, 1)
        RETURNING id INTO v_act1_id;
        
        INSERT INTO catechism_act_translation (act_id, language_code, title, description)
        VALUES (v_act1_id, 'en', 'The Creed', 'What we believe');
        
        -- Create Act 2: Sacraments
        INSERT INTO catechism_act (booklet_id, act_number, display_order)
        VALUES (v_booklet_id, 2, 2)
        RETURNING id INTO v_act2_id;
        
        INSERT INTO catechism_act_translation (act_id, language_code, title, description)
        VALUES (v_act2_id, 'en', 'The Sacraments', 'How we worship');
        
        -- Create Subtitle under Act 1
        INSERT INTO catechism_act_subtitle (act_id, subtitle_number, display_order)
        VALUES (v_act1_id, 1, 1)
        RETURNING id INTO v_subtitle1_id;
        
        INSERT INTO catechism_act_subtitle_translation (subtitle_id, language_code, title)
        VALUES (v_subtitle1_id, 'en', 'God the Father Almighty');
    END IF;
END $$;
*/
