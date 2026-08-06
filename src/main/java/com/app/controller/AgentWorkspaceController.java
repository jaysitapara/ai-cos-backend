package com.app.controller;

import com.app.dto.agent.ArtifactResponse;
import com.app.service.AgentWorkspaceService;
import com.app.dto.agent.CreateWorkspaceSessionRequest;
import com.app.dto.agent.ExecutionProgressResponse;
import com.app.dto.agent.ImplementationPlanResponse;
import com.app.dto.agent.PlanApprovalRequest;
import com.app.dto.agent.WorkspaceSessionResponse;
import com.app.entity.UserEntity;
import com.app.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/v1/agent-workspace")
@RequiredArgsConstructor
public class AgentWorkspaceController {

    private final AgentWorkspaceService workspaceService;
    private final UserRepository userRepository;

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

    @GetMapping("/sessions/{publicId}/export")
    public ResponseEntity<byte[]> exportDeliverablesZip(@PathVariable UUID publicId) throws IOException {
        List<ArtifactResponse> artifacts = workspaceService.getArtifacts(publicId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (ArtifactResponse artifact : artifacts) {
                ZipEntry entry = new ZipEntry(artifact.getFilePath());
                zos.putNextEntry(entry);
                zos.write(artifact.getContent().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }

        byte[] zipBytes = baos.toByteArray();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ai-cos-deliverables-" + publicId + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipBytes);
    }

    private UserEntity getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return userRepository.findAll().stream().findFirst().orElse(null);
        }
        String email = authentication.getName();
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));
    }
}
