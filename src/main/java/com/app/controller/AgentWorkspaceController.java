package com.app.controller;

import com.app.dto.agent.ArtifactResponse;
import com.app.service.AgentWorkspaceService;
import com.app.service.AiOutputSanitizerService;
import com.app.dto.agent.CreateWorkspaceSessionRequest;
import com.app.dto.agent.ExecutionProgressResponse;
import com.app.dto.agent.ImplementationPlanResponse;
import com.app.dto.agent.PlanApprovalRequest;
import com.app.dto.agent.WorkspaceSessionResponse;
import com.app.entity.UserEntity;
import com.app.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping(com.app.common.ApiConstants.API_V1 + "/agent-workspace")
@RequiredArgsConstructor
public class AgentWorkspaceController {

    private final AgentWorkspaceService workspaceService;
    private final UserRepository userRepository;
    private final AiOutputSanitizerService sanitizerService;

    @PostMapping("/sessions")
    public ResponseEntity<WorkspaceSessionResponse> createSession(
            @Valid @RequestBody CreateWorkspaceSessionRequest request,
            Authentication authentication) {

        UserEntity user = getAuthenticatedUser(authentication);
        WorkspaceSessionResponse response = workspaceService.createSession(request, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions/{publicId}")
    public ResponseEntity<WorkspaceSessionResponse> getSession(@PathVariable UUID publicId) {
        return ResponseEntity.ok(workspaceService.getSession(publicId));
    }

    @GetMapping("/sessions/{publicId}/plan")
    public ResponseEntity<ImplementationPlanResponse> getPlan(@PathVariable UUID publicId) {
        return ResponseEntity.ok(workspaceService.getPlan(publicId));
    }

    @PostMapping("/sessions/{publicId}/approve")
    public ResponseEntity<WorkspaceSessionResponse> approvePlan(
            @PathVariable UUID publicId,
            @Valid @RequestBody PlanApprovalRequest request) {

        return ResponseEntity.ok(workspaceService.approvePlan(publicId, request));
    }

    @GetMapping("/sessions/{publicId}/progress")
    public ResponseEntity<ExecutionProgressResponse> getProgress(@PathVariable UUID publicId) {
        return ResponseEntity.ok(workspaceService.getProgress(publicId));
    }

    @GetMapping("/sessions/{publicId}/artifacts")
    public ResponseEntity<List<ArtifactResponse>> getArtifacts(@PathVariable UUID publicId) {
        return ResponseEntity.ok(workspaceService.getArtifacts(publicId));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<WorkspaceSessionResponse>> listSessions(Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(workspaceService.listUserSessions(user));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/sessions/{publicId}")
    public ResponseEntity<Void> deleteSession(@PathVariable UUID publicId) {
        workspaceService.deleteSession(publicId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Phase 7.2 — ZIP Export with Final Sanitization Guard
     *
     * Before each artifact is written to the ZIP archive:
     *  1. Content is re-sanitized (strips any residual provider metadata)
     *  2. Content is checked for minimum viable length (>= 10 chars)
     *  3. Empty or suspiciously tiny content is skipped with a warning
     *
     * This is the LAST line of defense — even if something slipped through the
     * DB persistence layer, the ZIP will never contain raw AI metadata.
     */
    @GetMapping({"/sessions/{publicId}/export", "/sessions/{publicId}/export-zip"})
    public ResponseEntity<byte[]> exportDeliverablesZip(@PathVariable UUID publicId) throws IOException {
        List<ArtifactResponse> artifacts = workspaceService.getArtifacts(publicId);

        int included = 0;
        int skipped = 0;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.setComment("AI-COS Generated Project — Phase 7.2 Sanitized");

            for (ArtifactResponse artifact : artifacts) {
                String rawContent = artifact.getContent();
                if (rawContent == null) {
                    log.warn("ZIP SKIP [null content]: {}", artifact.getFilePath());
                    skipped++;
                    continue;
                }

                // Final sanitization pass — idempotent on already-clean content
                String sanitizedContent = sanitizerService.sanitizeOutput(rawContent);

                if (sanitizedContent.isBlank()) {
                    log.warn("ZIP SKIP [blank after sanitize]: {}", artifact.getFilePath());
                    skipped++;
                    continue;
                }

                // Guard: content suspiciously short (< 10 chars) for non-trivial types
                if (sanitizedContent.length() < 10) {
                    log.warn("ZIP SKIP [suspiciously short ({} chars)]: {}", sanitizedContent.length(), artifact.getFilePath());
                    skipped++;
                    continue;
                }

                // Guard: content contains known raw metadata signatures
                if (containsRawMetadataSignature(sanitizedContent)) {
                    log.error("ZIP BLOCK [raw metadata detected in sanitized content]: {} — SKIPPING to protect ZIP integrity",
                              artifact.getFilePath());
                    skipped++;
                    continue;
                }

                ZipEntry entry = new ZipEntry(artifact.getFilePath());
                zos.putNextEntry(entry);
                zos.write(sanitizedContent.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
                included++;
            }
        }

        log.info("ZIP export for session {}: {} files included, {} files skipped", publicId, included, skipped);
        byte[] zipBytes = baos.toByteArray();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ai-cos-deliverables-" + publicId + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipBytes);
    }

    /**
     * Detects known raw AI metadata signatures that should NEVER appear in exported files.
     * Returns true if the content appears to still contain unstripped provider metadata.
     */
    private boolean containsRawMetadataSignature(String content) {
        if (content == null) return false;
        return content.contains("=== WORKSPACE SESSION CONTEXT ===")
            || content.contains("=== SYSTEM PROMPT ===")
            || content.contains("=== AGENT INSTRUCTION ===")
            || content.contains("Processed prompt:")
            || content.contains("[LocalAI")
            || content.contains("[Gemini")
            || content.contains("[Groq")
            || content.contains("[OpenAI")
            || content.contains("[Anthropic")
            || content.contains("Goal Prompt:");
    }

    private UserEntity getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return userRepository.findAll().stream().findFirst().orElse(null);
        }
        String name = authentication.getName();
        try {
            UUID publicId = UUID.fromString(name);
            return userRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                    .orElseGet(() -> userRepository.findByEmailAndDeletedAtIsNull(name)
                            .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null)));
        } catch (IllegalArgumentException e) {
            return userRepository.findByEmailAndDeletedAtIsNull(name)
                    .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));
        }
    }
}
