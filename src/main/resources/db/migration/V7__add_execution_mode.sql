-- V7__add_execution_mode.sql
-- Add ExecutionMode to agent_workspace_sessions table for Auto vs Manual mode configuration

ALTER TABLE agent_workspace_sessions
ADD COLUMN execution_mode VARCHAR(50) NOT NULL DEFAULT 'AUTO';

CREATE INDEX idx_agent_sessions_execution_mode ON agent_workspace_sessions(execution_mode);
