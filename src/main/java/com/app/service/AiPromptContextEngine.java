package com.app.service;

import com.app.entity.AgentWorkspaceArtifactEntity;
import com.app.entity.AgentWorkspaceSessionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPromptContextEngine {

    private final AgentPromptEngine agentPromptEngine;

    private static final int MAX_CONTEXT_CHARS = 24000; // ~6000 tokens

    private static final Pattern SECRET_PATTERN = Pattern.compile(
        "(?i)(api[_-]?key|secret|password|bearer\\s+[a-zA-Z0-9._-]+|eyJ[a-zA-Z0-9._-]+)\\s*[:=]\\s*['\"]?[a-zA-Z0-9._-]+['\"]?"
    );

    /**
     * Builds context-injected, security-sanitized, token-budgeted prompt for an agent task.
     */
    public String buildContextualPrompt(
            AgentWorkspaceSessionEntity session,
            String agentRole,
            String taskTitle,
            List<AgentWorkspaceArtifactEntity> existingArtifacts) {

        String basePrompt = agentPromptEngine.buildPromptForAgent(agentRole, taskTitle, session.getGoalPrompt(), session.getProjectType());

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("=== WORKSPACE SESSION CONTEXT ===\n");
        contextBuilder.append("Goal Prompt: ").append(session.getGoalPrompt()).append("\n");
        contextBuilder.append("Project Type: ").append(session.getProjectType()).append("\n");
        contextBuilder.append("Complexity: ").append(session.getComplexity()).append("\n\n");

        if (existingArtifacts != null && !existingArtifacts.isEmpty()) {
            contextBuilder.append("=== PREVIOUS AGENT OUTPUTS & DELIVERABLES ===\n");
            for (AgentWorkspaceArtifactEntity artifact : existingArtifacts) {
                contextBuilder.append("Artifact: ").append(artifact.getFileName())
                    .append(" (Role: ").append(artifact.getAgentRole()).append(")\n");
                String contentSnippet = artifact.getContent();
                if (contentSnippet != null && contentSnippet.length() > 1000) {
                    contentSnippet = contentSnippet.substring(0, 1000) + "\n... [truncated for token budget]";
                }
                contextBuilder.append(contentSnippet).append("\n\n");
            }
        }

        contextBuilder.append("=== AGENT INSTRUCTION ===\n");
        contextBuilder.append(basePrompt);

        String fullContextPrompt = contextBuilder.toString();

        // Enforce Token Budget
        if (fullContextPrompt.length() > MAX_CONTEXT_CHARS) {
            log.info("Prompt exceeds token budget limit ({}/{} chars). Truncating context.", fullContextPrompt.length(), MAX_CONTEXT_CHARS);
            fullContextPrompt = fullContextPrompt.substring(0, MAX_CONTEXT_CHARS) + "\n\n... [Context truncated to fit token budget]";
        }

        // Security Secret Sanitization (PART 1.8)
        fullContextPrompt = sanitizeSecrets(fullContextPrompt);

        return fullContextPrompt;
    }

    /**
     * Sanitizes sensitive secrets, API keys, and JWT tokens from prompt text before LLM transmission.
     */
    public String sanitizeSecrets(String text) {
        if (text == null) return "";
        return SECRET_PATTERN.matcher(text).replaceAll("[REDACTED_SECRET]");
    }
}
