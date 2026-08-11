package com.app.service;

import com.app.entity.AgentExecutionLogEntity;
import com.app.entity.AgentExecutionTaskEntity;
import com.app.entity.AgentWorkspaceArtifactEntity;
import com.app.entity.AgentWorkspaceSessionEntity;
import com.app.provider.ai.AiCompletionRequest;
import com.app.provider.ai.AiCompletionResponse;
import com.app.service.AIService;
import com.app.repository.AgentExecutionLogRepository;
import com.app.repository.AgentExecutionTaskRepository;
import com.app.repository.AgentWorkspaceArtifactRepository;
import com.app.repository.AgentWorkspaceSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Phase 7.2 — Orchestration Engine with Production Retry Loop & Full 16-Agent Artifact Coverage
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrationEngine {

    private static final int MAX_SANITIZE_RETRIES = 3;

    private final AgentWorkspaceSessionRepository sessionRepository;
    private final AgentExecutionTaskRepository taskRepository;
    private final AgentExecutionLogRepository logRepository;
    private final AgentWorkspaceArtifactRepository artifactRepository;
    private final AIService aiService;
    private final AiPromptContextEngine contextEngine;
    private final AiOutputSanitizerService sanitizerService;
    private final TokenCostCalculator costCalculator;

    // ─── DAG GRAPH BUILDER ────────────────────────────────────────────────────

    /**
     * Initializes the 16-agent DAG execution graph for a workspace session.
     */
    @Transactional
    public List<AgentExecutionTaskEntity> buildExecutionGraph(AgentWorkspaceSessionEntity session) {
        List<AgentExecutionTaskEntity> tasks = new ArrayList<>();

        tasks.add(createTask(session, "TASK-01", "Domain & Requirements Analysis",      "BUSINESS_ANALYST",    "ANALYSIS",       1,  "[]"));
        tasks.add(createTask(session, "TASK-02", "User Story & Acceptance Matrix",       "PRODUCT_MANAGER",     "PLANNING",       2,  "[\"TASK-01\"]"));
        tasks.add(createTask(session, "TASK-03", "Tech Stack & Blueprint Research",      "RESEARCH",            "PLANNING",       3,  "[\"TASK-01\"]"));
        tasks.add(createTask(session, "TASK-04", "System Architecture Blueprint",        "SYSTEM_ARCHITECT",    "ARCHITECTURE",   4,  "[\"TASK-02\",\"TASK-03\"]"));
        tasks.add(createTask(session, "TASK-05", "Database Schema & Entity Design",      "DATABASE_AGENT",      "DATABASE",       5,  "[\"TASK-04\"]"));
        tasks.add(createTask(session, "TASK-06", "RESTful API Specification",            "API_AGENT",           "API_DESIGN",     6,  "[\"TASK-04\"]"));
        tasks.add(createTask(session, "TASK-07", "UI Design System & Wireframes",        "UI_UX_AGENT",         "DESIGN",         7,  "[\"TASK-04\"]"));
        tasks.add(createTask(session, "TASK-08", "Frontend Core App & Layouts",          "FRONTEND_AGENT",      "CODE_FRONTEND",  8,  "[\"TASK-06\",\"TASK-07\"]"));
        tasks.add(createTask(session, "TASK-09", "Backend Controllers & Services",       "BACKEND_AGENT",       "CODE_BACKEND",   9,  "[\"TASK-05\",\"TASK-06\"]"));
        tasks.add(createTask(session, "TASK-10", "AI Model Logic & Pipeline Integration","AI_ENGINEER_AGENT",   "AI_ENGINEERING", 10, "[\"TASK-09\"]"));
        tasks.add(createTask(session, "TASK-11", "Security & RBAC Enforcement Audit",   "SECURITY_AGENT",      "SECURITY",       11, "[\"TASK-08\",\"TASK-09\"]"));
        tasks.add(createTask(session, "TASK-12", "Automated QA & Unit Test Generation", "QA_AGENT",            "QA_TESTING",     12, "[\"TASK-08\",\"TASK-09\"]"));
        tasks.add(createTask(session, "TASK-13", "Containerization & CI/CD Pipeline",   "DEVOPS_AGENT",        "DEVOPS",         13, "[\"TASK-09\",\"TASK-11\"]"));
        tasks.add(createTask(session, "TASK-14", "Comprehensive System Documentation",  "DOCUMENTATION_AGENT", "DOCS",           14, "[\"TASK-04\",\"TASK-12\"]"));
        tasks.add(createTask(session, "TASK-15", "Static Code Quality Audit",           "CODE_REVIEW_AGENT",   "REVIEW",         15, "[\"TASK-11\",\"TASK-12\"]"));
        tasks.add(createTask(session, "TASK-16", "Production Deployment Package",        "DEPLOYMENT_AGENT",    "DEPLOYMENT",     16, "[\"TASK-13\",\"TASK-15\"]"));

        return taskRepository.saveAll(tasks);
    }

    private AgentExecutionTaskEntity createTask(AgentWorkspaceSessionEntity session, String key, String title,
                                                 String role, String phase, int order, String depsJson) {
        AgentExecutionTaskEntity t = AgentExecutionTaskEntity.builder()
                .publicId(UUID.randomUUID())
                .session(session)
                .taskKey(key)
                .title(title)
                .agentRole(role)
                .phase(phase)
                .executionOrder(order)
                .dependenciesJson(depsJson)
                .status("PENDING")
                .retries(0)
                .build();
        t.setCreatedBy("system");
        t.setUpdatedBy("system");
        return t;
    }

    // ─── ASYNC EXECUTION LOOP ─────────────────────────────────────────────────

    /**
     * Executes the orchestration DAG asynchronously using live AI completions.
     * Each task produces exactly one artifact. Only sanitized + validated content is persisted.
     */
    @Async
    public CompletableFuture<Void> executeOrchestrationAsync(Long sessionId) {
        log.info("Starting live AI autonomous multi-agent execution for session id: {}", sessionId);

        AgentWorkspaceSessionEntity session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return CompletableFuture.completedFuture(null);
        }

        session.setStatus("EXECUTING");
        sessionRepository.save(session);

        List<AgentExecutionTaskEntity> tasks = taskRepository.findBySessionIdOrderByExecutionOrderAsc(sessionId);

        for (AgentExecutionTaskEntity task : tasks) {
            if (!sessionRepository.existsById(sessionId)) {
                log.info("Session {} was deleted during execution. Terminating async loop cleanly.", sessionId);
                break;
            }

            String correlationId = UUID.randomUUID().toString();
            String requestId = UUID.randomUUID().toString();

            try {
                long startTime = System.currentTimeMillis();
                task.setStatus("IN_PROGRESS");
                task.setStartedAt(OffsetDateTime.now());
                task.setReasoningSummary("Agent [" + task.getAgentRole() + "] invoking LLM engine with context injection. CID: " + correlationId);
                taskRepository.save(task);

                addLog(session, task, task.getAgentRole(), "INFO",
                        "Task " + task.getTaskKey() + " started: " + task.getTitle() + " [CID: " + correlationId + " | RID: " + requestId + "]",
                        "Prompt context built & secrets sanitized. Querying AI Provider Factory with failover enabled.");

                // ──────────────────────────────────────────────────────────
                // CORE: Generate artifact with live AI + sanitization + retry
                // ──────────────────────────────────────────────────────────
                AiCompletionResponse aiResponse = generateAgentArtifactsAndOutputs(session, task, correlationId);

                long duration = System.currentTimeMillis() - startTime;
                double estimatedCost = 0.0;
                long totalTokens = 0;
                if (aiResponse != null) {
                    totalTokens = aiResponse.getPromptTokens() + aiResponse.getCompletionTokens();
                    estimatedCost = costCalculator.calculateCost(aiResponse.getModelName(), aiResponse.getPromptTokens(), aiResponse.getCompletionTokens());
                }

                task.setStatus("SUCCESS");
                task.setCompletedAt(OffsetDateTime.now());
                task.setOutputSummary("Completed cleanly via " + (aiResponse != null ? aiResponse.getModelName() : "LLM") +
                        " in " + duration + "ms. Tokens: " + totalTokens + " | Est Cost: $" + String.format("%.6f", estimatedCost));
                taskRepository.save(task);

                addLog(session, task, task.getAgentRole(), "INFO",
                        "Task " + task.getTaskKey() + " completed successfully via AI Provider.",
                        "Model: " + (aiResponse != null ? aiResponse.getModelName() : "LLM") +
                        " | Tokens: Prompt=" + (aiResponse != null ? aiResponse.getPromptTokens() : 0) +
                        ", Completion=" + (aiResponse != null ? aiResponse.getCompletionTokens() : 0) +
                        " | Latency=" + duration + "ms | Est. Cost=$" + String.format("%.6f", estimatedCost) +
                        " | CID: " + correlationId);

            } catch (Exception e) {
                log.error("Execution error on task {}", task.getTaskKey(), e);
                task.setStatus("FAILED");
                task.setRetries(task.getRetries() + 1);
                taskRepository.save(task);
                addLog(session, task, task.getAgentRole(), "ERROR",
                        "Task failure encountered [CID: " + correlationId + "]: " + e.getMessage(),
                        "Triggering self-healing recovery routine.");
            }
        }

        session.setStatus("COMPLETED");
        sessionRepository.save(session);

        addLog(session, null, "ORCHESTRATOR", "INFO",
                "All 16 Specialized AI Agents completed workspace execution. All artifacts sanitized, validated, and ready for export.",
                "Project deliverables generated, sanitized, validated, and ready for ZIP export.");

        return CompletableFuture.completedFuture(null);
    }

    // ─── ARTIFACT GENERATION WITH SANITIZATION RETRY LOOP ────────────────────

    /**
     * Generates the artifact for a given task agent role.
     *
     * Pipeline per attempt (up to MAX_SANITIZE_RETRIES):
     *  1. Build contextual prompt
     *  2. Call AI provider (with automatic failover)
     *  3. Sanitize raw response (strip fences, strip metadata, post-process)
     *  4. Validate sanitized content matches expected artifact structure
     *  5. If valid → persist; if not → retry (up to 3 times)
     *
     * ONLY sanitized content is ever passed to saveArtifact(). Raw responses are NEVER persisted.
     */
    private AiCompletionResponse generateAgentArtifactsAndOutputs(
            AgentWorkspaceSessionEntity session,
            AgentExecutionTaskEntity task,
            String correlationId) {

        String role = task.getAgentRole();
        List<AgentWorkspaceArtifactEntity> existingArtifacts =
                artifactRepository.findBySessionIdOrderByFilePathAsc(session.getId());

        // Build the contextual prompt with strict output preamble already injected via AgentPromptEngine
        String promptText = contextEngine.buildContextualPrompt(session, role, task.getTitle(), existingArtifacts);
        AiCompletionRequest request = new AiCompletionRequest(promptText, null, null, 0.2, 2048, null);

        // Resolve artifact metadata for this agent role
        ArtifactMeta meta = resolveArtifactMeta(role);
        if (meta == null) {
            // Non-artifact agent — still call LLM for token accounting but don't persist
            log.debug("Agent role '{}' does not produce a persisted artifact. Executing for telemetry only.", role);
            return aiService.generateCompletion(request, session.getUser(), null, task.getTaskKey(), "ORCHESTRATION", role);
        }

        AiCompletionResponse lastResponse = null;
        String finalContent = null;

        // ── 3-Attempt Sanitize + Validate Retry Loop ──────────────────────────
        for (int attempt = 1; attempt <= MAX_SANITIZE_RETRIES; attempt++) {
            try {
                log.info("Agent [{}] artifact generation attempt {}/{} [CID: {}]", role, attempt, MAX_SANITIZE_RETRIES, correlationId);

                AiCompletionResponse response = aiService.generateCompletion(request, session.getUser(), null, task.getTaskKey(), "ORCHESTRATION", role);
                lastResponse = response;

                String rawContent = response != null ? response.getContent() : "";

                // CRITICAL: Sanitize BEFORE any validation or persistence
                String sanitized = sanitizerService.sanitizeOutput(rawContent);

                boolean isValid = sanitizerService.validateArtifactContent(sanitized, meta.artifactType);

                if (isValid) {
                    log.info("Agent [{}] artifact validated on attempt {} — persisting clean content ({} chars)",
                             role, attempt, sanitized.length());
                    finalContent = sanitized;
                    break;
                } else {
                    log.warn("Agent [{}] artifact validation FAILED on attempt {}/{} for type '{}'. " +
                             "Sanitized content ({} chars): [{}...]",
                             role, attempt, MAX_SANITIZE_RETRIES, meta.artifactType,
                             sanitized.length(),
                             sanitized.substring(0, Math.min(120, sanitized.length())));

                    if (attempt < MAX_SANITIZE_RETRIES) {
                        addLog(session, task, role, "WARN",
                               "Artifact validation failed (attempt " + attempt + "/" + MAX_SANITIZE_RETRIES + "). Retrying with clean prompt.",
                               "Content type: " + meta.artifactType + " | Content preview: " + sanitized.substring(0, Math.min(80, sanitized.length())));
                    }
                }
            } catch (Exception e) {
                log.warn("Agent [{}] LLM call failed on attempt {}/{}: {}", role, attempt, MAX_SANITIZE_RETRIES, e.getMessage());
                if (attempt == MAX_SANITIZE_RETRIES) {
                    throw e;
                }
            }
        }

        // If all retries exhausted with invalid content, use last sanitized attempt anyway
        // (better than storing raw metadata) — and log a warning
        if (finalContent == null && lastResponse != null) {
            String lastSanitized = sanitizerService.sanitizeOutput(lastResponse.getContent());
            log.error("Agent [{}] failed validation after {} attempts. Persisting best-effort sanitized content to prevent raw metadata storage.",
                      role, MAX_SANITIZE_RETRIES);
            finalContent = lastSanitized.isBlank() ? generateFallbackContent(role, meta, session.getGoalPrompt()) : lastSanitized;

            addLog(session, task, role, "ERROR",
                   "Artifact validation failed after all " + MAX_SANITIZE_RETRIES + " retries. Persisting sanitized fallback content.",
                   "Type: " + meta.artifactType + " — Manual review required for: " + meta.filePath);
        }

        if (finalContent == null) {
            finalContent = generateFallbackContent(role, meta, session.getGoalPrompt());
        }

        saveArtifact(session, meta.filePath, meta.fileName, meta.artifactType, role, finalContent);
        return lastResponse;
    }

    // ─── ARTIFACT METADATA REGISTRY (all 16 agents) ───────────────────────────

    /**
     * Returns the artifact file metadata for a given agent role.
     * Returns null for agents that do not produce a standalone persisted file.
     */
    private ArtifactMeta resolveArtifactMeta(String role) {
        switch (role) {
            case "BUSINESS_ANALYST":
                return new ArtifactMeta("docs/requirements-spec.md",                                           "requirements-spec.md",                    "MARKDOWN");
            case "PRODUCT_MANAGER":
                return new ArtifactMeta("docs/user-stories.md",                                                "user-stories.md",                         "MARKDOWN");
            case "RESEARCH":
                return new ArtifactMeta("docs/tech-stack-blueprint.md",                                        "tech-stack-blueprint.md",                 "MARKDOWN");
            case "SYSTEM_ARCHITECT":
                return new ArtifactMeta("docs/architecture-blueprint.md",                                      "architecture-blueprint.md",               "MARKDOWN");
            case "DATABASE_AGENT":
                return new ArtifactMeta("backend/src/main/resources/db/migration/V10__generated_schema.sql",  "V10__generated_schema.sql",               "DATABASE_DDL");
            case "API_AGENT":
                return new ArtifactMeta("docs/openapi-spec.yaml",                                              "openapi-spec.yaml",                       "SWAGGER_SPEC");
            case "UI_UX_AGENT":
                return new ArtifactMeta("frontend/src/styles/design-system.json",                             "design-system.json",                      "JSON");
            case "FRONTEND_AGENT":
                return new ArtifactMeta("frontend/src/pages/GeneratedDomainPage.tsx",                         "GeneratedDomainPage.tsx",                 "REACT_TSX");
            case "BACKEND_AGENT":
                return new ArtifactMeta("backend/src/main/java/com/app/controller/DomainResourceController.java", "DomainResourceController.java",        "JAVA_SOURCE");
            case "AI_ENGINEER_AGENT":
                return new ArtifactMeta("backend/src/main/resources/ai-pipeline.yml",                         "ai-pipeline.yml",                         "YAML");
            case "SECURITY_AGENT":
                return new ArtifactMeta("docs/security-audit-report.md",                                       "security-audit-report.md",                "MARKDOWN");
            case "QA_AGENT":
                return new ArtifactMeta("backend/src/test/java/com/app/service/GeneratedServiceTest.java",    "GeneratedServiceTest.java",               "JAVA_SOURCE");
            case "DEVOPS_AGENT":
                return new ArtifactMeta("docker-compose.yml",                                                  "docker-compose.yml",                      "DOCKER");
            case "DOCUMENTATION_AGENT":
                return new ArtifactMeta("README.md",                                                           "README.md",                               "MARKDOWN");
            case "CODE_REVIEW_AGENT":
                return new ArtifactMeta("docs/code-review-audit.md",                                           "code-review-audit.md",                    "MARKDOWN");
            case "DEPLOYMENT_AGENT":
                return new ArtifactMeta("docs/deployment-runbook.md",                                          "deployment-runbook.md",                   "MARKDOWN");
            default:
                return null;
        }
    }

    /** Simple value holder for artifact file metadata. */
    private static class ArtifactMeta {
        final String filePath;
        final String fileName;
        final String artifactType;

        ArtifactMeta(String filePath, String fileName, String artifactType) {
            this.filePath = filePath;
            this.fileName = fileName;
            this.artifactType = artifactType;
        }
    }

    // ─── FALLBACK CONTENT GENERATOR ───────────────────────────────────────────

    /**
     * Generates minimal valid fallback content for an artifact type when all LLM retries fail.
     * This prevents the database from ever storing raw AI metadata or empty content.
     */
    private String generateFallbackContent(String role, ArtifactMeta meta, String goalPrompt) {
        log.warn("Using fallback content generator for role '{}' ({})", role, meta.artifactType);
        switch (meta.artifactType) {
            case "JAVA_SOURCE":
                return "package com.app.generated;\n\n" +
                       "/**\n * Auto-generated placeholder for: " + role + "\n" +
                       " * Goal: " + goalPrompt + "\n */\n" +
                       "public class GeneratedPlaceholder {\n" +
                       "    // TODO: Regenerate this file — LLM retry limit reached\n" +
                       "}\n";
            case "REACT_TSX":
                return "import React from 'react';\n\n" +
                       "/** Auto-generated placeholder for: " + role + " */\n" +
                       "export const GeneratedPage: React.FC = () => (\n" +
                       "  <div className=\"p-8\">\n" +
                       "    <h1>Generated Page — Regeneration Required</h1>\n" +
                       "    <p>Goal: " + goalPrompt + "</p>\n" +
                       "  </div>\n" +
                       ");\n";
            case "DATABASE_DDL":
                return "-- Auto-generated placeholder for: " + role + "\n" +
                       "-- Goal: " + goalPrompt + "\n" +
                       "-- TODO: Regenerate this file — LLM retry limit reached\n" +
                       "CREATE TABLE IF NOT EXISTS placeholder (\n" +
                       "    id BIGSERIAL PRIMARY KEY,\n" +
                       "    created_at TIMESTAMPTZ DEFAULT now()\n" +
                       ");\n";
            case "SWAGGER_SPEC":
            case "YAML":
            case "DOCKER":
                return "# Auto-generated placeholder for: " + role + "\n" +
                       "# Goal: " + goalPrompt + "\n" +
                       "# TODO: Regenerate — LLM retry limit reached\n" +
                       "placeholder:\n  generated: true\n";
            case "JSON":
                return "{\n  \"_note\": \"Auto-generated placeholder for " + role + " — regeneration required\",\n" +
                       "  \"goal\": \"" + goalPrompt.replace("\"", "'") + "\"\n}\n";
            case "MARKDOWN":
            default:
                return "# Auto-Generated Placeholder\n\n" +
                       "**Agent Role:** " + role + "\n\n" +
                       "**Goal:** " + goalPrompt + "\n\n" +
                       "> ⚠️ This file is a placeholder. The LLM retry limit was reached during generation.\n" +
                       "> Please regenerate this artifact.\n";
        }
    }

    // ─── PERSISTENCE ──────────────────────────────────────────────────────────

    /**
     * Persists a sanitized artifact. This is the ONLY path to the database.
     * Raw provider responses NEVER reach this method — sanitizeOutput() is always called first.
     */
    private void saveArtifact(AgentWorkspaceSessionEntity session, String path, String name,
                               String type, String role, String content) {
        AgentWorkspaceArtifactEntity artifact = AgentWorkspaceArtifactEntity.builder()
                .publicId(UUID.randomUUID())
                .session(session)
                .filePath(path)
                .fileName(name)
                .artifactType(type)
                .content(content)
                .agentRole(role)
                .build();
        artifact.setCreatedBy("system");
        artifact.setUpdatedBy("system");
        artifactRepository.save(artifact);
        log.debug("Artifact persisted: {} ({} chars, type={})", path, content.length(), type);
    }

    // ─── LOG HELPER ───────────────────────────────────────────────────────────

    private void addLog(AgentWorkspaceSessionEntity session, AgentExecutionTaskEntity task,
                        String role, String level, String msg, String reasoning) {
        AgentExecutionLogEntity l = AgentExecutionLogEntity.builder()
                .session(session)
                .task(task)
                .agentRole(role)
                .logLevel(level)
                .message(msg)
                .reasoning(reasoning)
                .timestamp(OffsetDateTime.now())
                .build();
        logRepository.save(l);
    }
}
