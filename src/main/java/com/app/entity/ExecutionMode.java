package com.app.entity;

/**
 * Execution Mode options for AI-COS workspace sessions.
 */
public enum ExecutionMode {
    /**
     * Automatic Execution:
     * Platform automatically generates plan, approves plan internally,
     * and immediately launches all 16 AI agents and artifact creation.
     */
    AUTO,

    /**
     * Manual Execution:
     * Platform generates plan, pauses at AWAITING_APPROVAL, and waits
     * for explicit user consent before launching multi-agent execution.
     */
    MANUAL
}
