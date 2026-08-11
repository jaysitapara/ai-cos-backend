-- V9__personal_brand_content_agent.sql
-- Database Migration for Personal Brand AI Content Agent Module

-- 1. BRANDS TABLE
CREATE TABLE IF NOT EXISTS brands (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    industry VARCHAR(100),
    positioning TEXT,
    target_audience TEXT,
    products_summary TEXT,
    services_summary TEXT,
    usp TEXT,
    website VARCHAR(255),
    brand_voice VARCHAR(100),
    writing_style TEXT,
    words_to_use TEXT,
    words_to_avoid TEXT,
    content_goals TEXT,
    brand_guidelines TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_brands_user_id ON brands(user_id);

-- 2. BRAND PRODUCTS TABLE
CREATE TABLE IF NOT EXISTS brand_products (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    brand_id BIGINT NOT NULL REFERENCES brands(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'PRODUCT',
    description TEXT,
    target_customer TEXT,
    key_features TEXT,
    price_range VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bp_brand_id ON brand_products(brand_id);

-- 3. BRAND KNOWLEDGE TABLE
CREATE TABLE IF NOT EXISTS brand_knowledge (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    brand_id BIGINT NOT NULL REFERENCES brands(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    source VARCHAR(100),
    type VARCHAR(50) NOT NULL DEFAULT 'IDENTITY',
    content TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bk_brand_id ON brand_knowledge(brand_id);

-- 4. CONTENT THREADS TABLE
CREATE TABLE IF NOT EXISTS content_threads (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    brand_id BIGINT NOT NULL REFERENCES brands(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    content_type VARCHAR(50) NOT NULL DEFAULT 'LINKEDIN_POST',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    current_step INT DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ct_user_brand ON content_threads(user_id, brand_id);

-- 5. CONTENT MESSAGES TABLE
CREATE TABLE IF NOT EXISTS content_messages (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    thread_id BIGINT NOT NULL REFERENCES content_threads(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    message_type VARCHAR(50) NOT NULL DEFAULT 'TEXT',
    content TEXT NOT NULL,
    metadata_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cm_thread_id ON content_messages(thread_id);

-- 6. CONTENT QUESTION STATES TABLE
CREATE TABLE IF NOT EXISTS content_question_states (
    id BIGSERIAL PRIMARY KEY,
    thread_id BIGINT NOT NULL REFERENCES content_threads(id) ON DELETE CASCADE,
    question_key VARCHAR(100) NOT NULL,
    question_title VARCHAR(255) NOT NULL,
    selected_option VARCHAR(100),
    custom_value TEXT,
    order_index INT DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cqs_thread_id ON content_question_states(thread_id);

-- 7. CONTENT BRIEFS TABLE
CREATE TABLE IF NOT EXISTS content_briefs (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    thread_id BIGINT NOT NULL REFERENCES content_threads(id) ON DELETE CASCADE,
    content_type VARCHAR(50) NOT NULL,
    topic TEXT,
    goal TEXT,
    target_audience TEXT,
    key_message TEXT,
    tone VARCHAR(100),
    cta TEXT,
    additional_instructions TEXT,
    structured_brief_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cb_thread_id ON content_briefs(thread_id);

-- 8. GENERATED CONTENTS TABLE
CREATE TABLE IF NOT EXISTS generated_contents (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    brand_id BIGINT NOT NULL REFERENCES brands(id) ON DELETE CASCADE,
    thread_id BIGINT REFERENCES content_threads(id) ON DELETE SET NULL,
    brief_id BIGINT REFERENCES content_briefs(id) ON DELETE SET NULL,
    content_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    current_version_id BIGINT,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_gc_user_brand ON generated_contents(user_id, brand_id);
CREATE INDEX IF NOT EXISTS idx_gc_status ON generated_contents(status);

-- 9. CONTENT VERSIONS TABLE
CREATE TABLE IF NOT EXISTS content_versions (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    content_id BIGINT NOT NULL REFERENCES generated_contents(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    parent_version_id BIGINT REFERENCES content_versions(id) ON DELETE SET NULL,
    body TEXT NOT NULL,
    change_source VARCHAR(50) NOT NULL DEFAULT 'AI_INITIAL',
    change_summary TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cv_content_id ON content_versions(content_id);

-- 10. USER MEMORIES TABLE
CREATE TABLE IF NOT EXISTS user_memories (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    memory_type VARCHAR(50) NOT NULL,
    value TEXT NOT NULL,
    source VARCHAR(50) NOT NULL DEFAULT 'USER_FEEDBACK',
    confidence DOUBLE PRECISION DEFAULT 1.0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_um_user_id ON user_memories(user_id);

-- 11. BRAND MEMORIES TABLE
CREATE TABLE IF NOT EXISTS brand_memories (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    brand_id BIGINT NOT NULL REFERENCES brands(id) ON DELETE CASCADE,
    memory_type VARCHAR(50) NOT NULL,
    value TEXT NOT NULL,
    source VARCHAR(50) NOT NULL DEFAULT 'USER_FEEDBACK',
    confidence DOUBLE PRECISION DEFAULT 1.0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bm_brand_id ON brand_memories(brand_id);

-- 12. APPROVED CONTENTS TABLE
CREATE TABLE IF NOT EXISTS approved_contents (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    brand_id BIGINT NOT NULL REFERENCES brands(id) ON DELETE CASCADE,
    content_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    final_content TEXT NOT NULL,
    performance_rating INT DEFAULT 5,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ac_brand_id ON approved_contents(brand_id);

-- 13. CONTENT FEEDBACKS TABLE
CREATE TABLE IF NOT EXISTS content_feedbacks (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    brand_id BIGINT NOT NULL REFERENCES brands(id) ON DELETE CASCADE,
    content_id BIGINT NOT NULL REFERENCES generated_contents(id) ON DELETE CASCADE,
    version_id BIGINT REFERENCES content_versions(id) ON DELETE SET NULL,
    feedback_type VARCHAR(30) NOT NULL DEFAULT 'COMMENT',
    rating INT,
    feedback_text TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cf_content_id ON content_feedbacks(content_id);
