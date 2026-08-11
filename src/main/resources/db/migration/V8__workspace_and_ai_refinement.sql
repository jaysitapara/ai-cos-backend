-- V8__workspace_and_ai_refinement.sql
-- Migration for AI provider architecture, dynamic requirement questions, user-scoped project outputs, versioning, comments, and telemetry.

-- 1. AI USAGE LOGS TABLE
CREATE TABLE IF NOT EXISTS ai_usage_logs (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    project_id BIGINT,
    job_id VARCHAR(100),
    request_id VARCHAR(100),
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    request_type VARCHAR(50),
    agent_role VARCHAR(50),
    prompt_tokens INT DEFAULT 0,
    completion_tokens INT DEFAULT 0,
    total_tokens INT DEFAULT 0,
    latency_ms BIGINT DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    error_message TEXT,
    estimated_cost NUMERIC(12, 6) DEFAULT 0.0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_usage_user_id ON ai_usage_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_usage_project_id ON ai_usage_logs(project_id);
CREATE INDEX IF NOT EXISTS idx_ai_usage_created_at ON ai_usage_logs(created_at);

-- 2. ENHANCE PROJECTS TABLE
ALTER TABLE projects ADD COLUMN IF NOT EXISTS user_id BIGINT REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE projects ADD COLUMN IF NOT EXISTS current_version_id BIGINT;
ALTER TABLE projects ADD COLUMN IF NOT EXISTS runtime_status VARCHAR(50) DEFAULT 'STOPPED';
ALTER TABLE projects ADD COLUMN IF NOT EXISTS runtime_port INT;
ALTER TABLE projects ADD COLUMN IF NOT EXISTS active_tech_stack_json TEXT;
ALTER TABLE projects ADD COLUMN IF NOT EXISTS preview_url VARCHAR(255);
ALTER TABLE projects ADD COLUMN IF NOT EXISTS last_run_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_projects_user_id ON projects(user_id);
CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status);

-- 3. PROJECT CREATION SESSIONS TABLE
CREATE TABLE IF NOT EXISTS project_creation_sessions (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id BIGINT REFERENCES projects(id) ON DELETE SET NULL,
    initial_prompt TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ANALYZING',
    current_step INT DEFAULT 1,
    total_steps INT DEFAULT 1,
    configuration_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pcs_user_id ON project_creation_sessions(user_id);

-- 4. PROJECT QUESTIONS TABLE
CREATE TABLE IF NOT EXISTS project_questions (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES project_creation_sessions(id) ON DELETE CASCADE,
    question_key VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    options_json TEXT NOT NULL,
    selected_option VARCHAR(100),
    custom_value TEXT,
    is_recommended BOOLEAN DEFAULT FALSE,
    recommendation_reason TEXT,
    order_index INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX IF NOT EXISTS idx_pq_session_id ON project_questions(session_id);

-- 5. PROJECT VERSIONS TABLE
CREATE TABLE IF NOT EXISTS project_versions (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    parent_version_id BIGINT REFERENCES project_versions(id) ON DELETE SET NULL,
    change_summary TEXT,
    user_comment_id BIGINT,
    changed_files_json TEXT,
    generation_metadata_json TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pv_project_id ON project_versions(project_id);
CREATE INDEX IF NOT EXISTS idx_pv_version_number ON project_versions(project_id, version_number);

-- 6. PROJECT FILES TABLE
CREATE TABLE IF NOT EXISTS project_files (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    version_id BIGINT REFERENCES project_versions(id) ON DELETE CASCADE,
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pf_project_id ON project_files(project_id);
CREATE INDEX IF NOT EXISTS idx_pf_version_id ON project_files(version_id);

-- 7. PROJECT JOBS TABLE
CREATE TABLE IF NOT EXISTS project_jobs (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    version_id BIGINT REFERENCES project_versions(id) ON DELETE SET NULL,
    job_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'QUEUED',
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    metadata_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pj_project_id ON project_jobs(project_id);

-- 8. PROJECT LOGS TABLE
CREATE TABLE IF NOT EXISTS project_logs (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    job_id BIGINT REFERENCES project_jobs(id) ON DELETE SET NULL,
    log_level VARCHAR(20) NOT NULL DEFAULT 'INFO',
    source VARCHAR(50) NOT NULL DEFAULT 'BUILD',
    message TEXT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pl_project_id ON project_logs(project_id);

-- 9. PROJECT COMMENTS TABLE
CREATE TABLE IF NOT EXISTS project_comments (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    version_id BIGINT REFERENCES project_versions(id) ON DELETE SET NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_path VARCHAR(500),
    line_number INT,
    comment_text TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    live_update_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pc_project_id ON project_comments(project_id);
CREATE INDEX IF NOT EXISTS idx_pc_user_id ON project_comments(user_id);
