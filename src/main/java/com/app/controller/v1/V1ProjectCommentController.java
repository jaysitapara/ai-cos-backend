package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.entity.ProjectCommentEntity;
import com.app.entity.ProjectEntity;
import com.app.entity.UserEntity;
import com.app.exception.ResourceNotFoundException;
import com.app.exception.UnauthorizedException;
import com.app.repository.ProjectCommentRepository;
import com.app.repository.ProjectRepository;
import com.app.repository.UserRepository;
import com.app.service.ProjectPatcherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(ApiConstants.API_V1 + "/projects/{projectId}/comments")
@RequiredArgsConstructor
@Tag(name = "V1 Comments & Live Updates", description = "Project Comments & Live Update Change Request REST APIs")
public class V1ProjectCommentController {

    private final ProjectRepository projectRepository;
    private final ProjectCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ProjectPatcherService patcherService;

    @Data
    public static class CreateCommentRequest {
        private String commentText;
        private String filePath;
        private Integer lineNumber;
        private Boolean liveUpdateEnabled;
    }

    @GetMapping
    @Operation(summary = "Get comments thread for project")
    public ResponseEntity<List<Map<String, Object>>> getProjectComments(@PathVariable UUID projectId, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectEntity project = projectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(projectId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        List<ProjectCommentEntity> comments = commentRepository.findByProjectIdOrderByCreatedAtDesc(project.getId());

        List<Map<String, Object>> response = comments.stream().map(c -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", c.getPublicId().toString());
            map.put("commentText", c.getCommentText());
            map.put("filePath", c.getFilePath() != null ? c.getFilePath() : "");
            map.put("lineNumber", c.getLineNumber() != null ? c.getLineNumber() : 0);
            map.put("status", c.getStatus());
            map.put("liveUpdateEnabled", c.getLiveUpdateEnabled());
            map.put("createdAt", c.getCreatedAt().toString());
            map.put("authorEmail", c.getUser().getEmail());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Submit a comment or Live Update change request")
    public ResponseEntity<Map<String, Object>> addComment(@PathVariable UUID projectId,
                                                          @RequestBody CreateCommentRequest request,
                                                          Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectEntity project = projectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(projectId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        boolean liveUpdate = Boolean.TRUE.equals(request.getLiveUpdateEnabled());

        ProjectCommentEntity comment = ProjectCommentEntity.builder()
                .publicId(UUID.randomUUID())
                .project(project)
                .user(user)
                .filePath(request.getFilePath())
                .lineNumber(request.getLineNumber())
                .commentText(request.getCommentText())
                .status(liveUpdate ? "PROCESSING" : "SUBMITTED")
                .liveUpdateEnabled(liveUpdate)
                .build();
        comment = commentRepository.save(comment);

        if (liveUpdate) {
            log.info("Triggering async Live Update patch for project {} comment {}", projectId, comment.getPublicId());
            patcherService.processLiveUpdateAsync(project.getId(), comment.getId(), user.getId());
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("commentId", comment.getPublicId().toString());
        response.put("status", comment.getStatus());
        response.put("liveUpdateEnabled", liveUpdate);
        response.put("message", liveUpdate ? "Live Update request queued for targeted incremental patch" : "Comment submitted successfully");

        return ResponseEntity.ok(response);
    }

    private UserEntity getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User authentication required");
        }
        String name = authentication.getName();
        try {
            UUID publicId = UUID.fromString(name);
            return userRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                    .orElseGet(() -> userRepository.findByEmailAndDeletedAtIsNull(name)
                            .orElseThrow(() -> new UnauthorizedException("Authenticated user not found")));
        } catch (IllegalArgumentException e) {
            return userRepository.findByEmailAndDeletedAtIsNull(name)
                    .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
        }
    }
}
