-- V10__content_intelligence_analytics.sql
-- Content Intelligence & Analytics Module Schema

CREATE TABLE IF NOT EXISTS content_analytics (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    brand_id BIGINT NOT NULL REFERENCES brands(id) ON DELETE CASCADE,
    content_id BIGINT UNIQUE NOT NULL REFERENCES generated_contents(id) ON DELETE CASCADE,
    content_version_id BIGINT REFERENCES content_versions(id) ON DELETE SET NULL,
    content_type VARCHAR(64) NOT NULL,
    topic VARCHAR(255),
    platform VARCHAR(64) DEFAULT 'OTHER',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    edit_count INT DEFAULT 0,
    version_count INT DEFAULT 1,
    performance_score DOUBLE PRECISION DEFAULT 0.0,
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP WITH TIME ZONE,
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS content_behavior_events (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    brand_id BIGINT NOT NULL REFERENCES brands(id) ON DELETE CASCADE,
    content_id BIGINT REFERENCES generated_contents(id) ON DELETE CASCADE,
    event_type VARCHAR(64) NOT NULL,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS content_performance_snapshots (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID UNIQUE NOT NULL,
    content_analytics_id BIGINT NOT NULL REFERENCES content_analytics(id) ON DELETE CASCADE,
    platform VARCHAR(64) NOT NULL,
    impressions BIGINT DEFAULT 0,
    reach BIGINT DEFAULT 0,
    likes BIGINT DEFAULT 0,
    comments BIGINT DEFAULT 0,
    shares BIGINT DEFAULT 0,
    saves BIGINT DEFAULT 0,
    clicks BIGINT DEFAULT 0,
    conversions BIGINT DEFAULT 0,
    engagement_rate DOUBLE PRECISION DEFAULT 0.0,
    snapshot_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS content_intelligence_insights (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    brand_id BIGINT NOT NULL REFERENCES brands(id) ON DELETE CASCADE,
    type VARCHAR(64) NOT NULL, -- PREFERENCE, STYLE, CONTENT_TYPE, TOPIC, TONE, AVERSION, LENGTH, PERFORMANCE
    insight_key VARCHAR(128) NOT NULL,
    insight_value TEXT NOT NULL,
    confidence VARCHAR(32) NOT NULL DEFAULT 'LOW', -- LOW, MEDIUM, HIGH
    confidence_score DOUBLE PRECISION DEFAULT 0.5,
    evidence_summary TEXT,
    evidence_count INT DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, WEAKENED, ARCHIVED
    scope VARCHAR(32) NOT NULL DEFAULT 'BRAND', -- BRAND, USER, CONTENT_TYPE
    content_type_scope VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Performance Indexes
CREATE INDEX idx_content_analytics_user_brand ON content_analytics(user_id, brand_id);
CREATE INDEX idx_content_analytics_type_status ON content_analytics(content_type, status);
CREATE INDEX idx_behavior_events_user_brand ON content_behavior_events(user_id, brand_id, event_type);
CREATE INDEX idx_performance_snapshots_analytics ON content_performance_snapshots(content_analytics_id);
CREATE INDEX idx_intelligence_user_brand ON content_intelligence_insights(user_id, brand_id, status);
