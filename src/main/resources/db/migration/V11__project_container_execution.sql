-- V11__project_container_execution.sql
-- Schema enhancements for isolated container project execution

ALTER TABLE projects
    ADD COLUMN IF NOT EXISTS container_id VARCHAR(128),
    ADD COLUMN IF NOT EXISTS runtime_port INT,
    ADD COLUMN IF NOT EXISTS execution_mode VARCHAR(32) DEFAULT 'container';

ALTER TABLE project_jobs
    ADD COLUMN IF NOT EXISTS container_id VARCHAR(128),
    ADD COLUMN IF NOT EXISTS runtime_port INT;

CREATE INDEX IF NOT EXISTS idx_projects_container_id ON projects(container_id);
CREATE INDEX IF NOT EXISTS idx_project_jobs_container_id ON project_jobs(container_id);
