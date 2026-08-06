-- V5__ai_agent_workspace_schema.sql
-- Autonomous AI Agent Workspace Schema Extension

CREATE TABLE agent_workspace_sessions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    user_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    goal_prompt TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ANALYZING',
    project_type VARCHAR(100) NULL,
    complexity VARCHAR(50) NULL,
    estimated_scope VARCHAR(100) NULL,
    uploaded_files_json TEXT NULL,
    assumptions_json TEXT NULL,
    missing_info_json TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    deleted_at TIMESTAMPTZ NULL,
    deleted_by VARCHAR(100) NULL
);

CREATE INDEX idx_agent_sessions_public_id ON agent_workspace_sessions(public_id);
CREATE INDEX idx_agent_sessions_user_id ON agent_workspace_sessions(user_id);
CREATE INDEX idx_agent_sessions_status ON agent_workspace_sessions(status);

CREATE TABLE agent_workspace_plans (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    session_id BIGINT NOT NULL REFERENCES agent_workspace_sessions(id) ON DELETE CASCADE,
    executive_summary TEXT NULL,
    business_requirements TEXT NULL,
    functional_requirements TEXT NULL,
    non_functional_requirements TEXT NULL,
    user_stories_json TEXT NULL,
    features_modules_json TEXT NULL,
    dependencies_json TEXT NULL,
    risks_json TEXT NULL,
    milestones_json TEXT NULL,
    system_architecture TEXT NULL,
    folder_structure TEXT NULL,
    database_design TEXT NULL,
    api_design TEXT NULL,
    er_diagram_mermaid TEXT NULL,
    tech_stack_json TEXT NULL,
    auth_flow TEXT NULL,
    deployment_architecture TEXT NULL,
    approval_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    approval_feedback TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    deleted_at TIMESTAMPTZ NULL,
    deleted_by VARCHAR(100) NULL
);

CREATE INDEX idx_agent_plans_public_id ON agent_workspace_plans(public_id);
CREATE INDEX idx_agent_plans_session_id ON agent_workspace_plans(session_id);

CREATE TABLE agent_execution_graph (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    session_id BIGINT NOT NULL REFERENCES agent_workspace_sessions(id) ON DELETE CASCADE,
    task_key VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    agent_role VARCHAR(100) NOT NULL,
    phase VARCHAR(100) NOT NULL,
    execution_order INT NOT NULL DEFAULT 1,
    dependencies_json TEXT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    retries INT NOT NULL DEFAULT 0,
    reasoning_summary TEXT NULL,
    output_summary TEXT NULL,
    started_at TIMESTAMPTZ NULL,
    completed_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    deleted_at TIMESTAMPTZ NULL,
    deleted_by VARCHAR(100) NULL
);

CREATE INDEX idx_agent_graph_session_id ON agent_execution_graph(session_id);
CREATE INDEX idx_agent_graph_status ON agent_execution_graph(status);

CREATE TABLE agent_execution_logs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES agent_workspace_sessions(id) ON DELETE CASCADE,
    task_id BIGINT NULL REFERENCES agent_execution_graph(id) ON DELETE SET NULL,
    agent_role VARCHAR(100) NOT NULL,
    log_level VARCHAR(20) NOT NULL DEFAULT 'INFO',
    message TEXT NOT NULL,
    reasoning TEXT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_agent_logs_session_id ON agent_execution_logs(session_id);

CREATE TABLE agent_workspace_artifacts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    session_id BIGINT NOT NULL REFERENCES agent_workspace_sessions(id) ON DELETE CASCADE,
    file_path VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    artifact_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    agent_role VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    deleted_at TIMESTAMPTZ NULL,
    deleted_by VARCHAR(100) NULL
);

CREATE INDEX idx_agent_artifacts_session_id ON agent_workspace_artifacts(session_id);
