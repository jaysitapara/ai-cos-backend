package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.entity.*;
import com.app.exception.ResourceNotFoundException;
import com.app.exception.UnauthorizedException;
import com.app.repository.*;
import com.app.service.ProjectExecutionPipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(ApiConstants.API_V1 + "/projects")
@RequiredArgsConstructor
@Tag(name = "V1 Projects", description = "User Project Workspace & Output Management REST APIs")
public class V1ProjectController {

    private final ProjectRepository projectRepository;
    private final ProjectVersionRepository versionRepository;
    private final ProjectFileRepository fileRepository;
    private final ProjectLogRepository logRepository;
    private final UserRepository userRepository;
    private final ProjectExecutionPipelineService executionPipelineService;

    @GetMapping
    @Operation(summary = "List projects owned by the authenticated user")
    public ResponseEntity<List<Map<String, Object>>> listUserProjects(Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        List<ProjectEntity> projects = projectRepository.findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(user.getId());

        List<Map<String, Object>> response = projects.stream().map(this::mapProjectSummary).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed project workspace information by ID")
    public ResponseEntity<Map<String, Object>> getProjectDetail(@PathVariable UUID id, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectEntity project = projectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));

        Map<String, Object> detail = mapProjectSummary(project);

        List<ProjectVersionEntity> versions = versionRepository.findByProjectIdOrderByVersionNumberDesc(project.getId());
        detail.put("versionCount", versions.size());
        
        List<Map<String, Object>> versionList = new ArrayList<>();
        for (ProjectVersionEntity v : versions) {
            Map<String, Object> vm = new LinkedHashMap<>();
            vm.put("versionNumber", v.getVersionNumber());
            vm.put("changeSummary", v.getChangeSummary() != null ? v.getChangeSummary() : "");
            vm.put("createdAt", v.getCreatedAt().toString());
            versionList.add(vm);
        }
        detail.put("versions", versionList);

        return ResponseEntity.ok(detail);
    }

    @PostMapping("/{id}/generate")
    @Operation(summary = "Trigger project generation & execution pipeline")
    public ResponseEntity<Map<String, Object>> triggerProjectGeneration(@PathVariable UUID id, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectEntity project = projectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));

        executionPipelineService.runExecutionPipelineAsync(project.getId(), user.getId());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("projectId", project.getPublicId().toString());
        res.put("status", "GENERATING");
        res.put("message", "Project generation pipeline triggered asynchronously");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}/files")
    @Operation(summary = "Get generated files for a project")
    public ResponseEntity<List<Map<String, Object>>> getProjectFiles(@PathVariable UUID id, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectEntity project = projectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));

        List<ProjectFileEntity> files = fileRepository.findByProjectIdAndIsDeletedFalseOrderByFilePathAsc(project.getId());

        List<Map<String, Object>> response = new ArrayList<>();
        for (ProjectFileEntity f : files) {
            Map<String, Object> fm = new LinkedHashMap<>();
            fm.put("id", f.getId());
            fm.put("filePath", f.getFilePath());
            fm.put("fileName", f.getFileName());
            fm.put("fileType", f.getFileType());
            fm.put("content", f.getContent());
            fm.put("updatedAt", f.getUpdatedAt().toString());
            response.add(fm);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/logs")
    @Operation(summary = "Get build and runtime process logs")
    public ResponseEntity<List<Map<String, Object>>> getProjectLogs(@PathVariable UUID id, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectEntity project = projectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));

        List<ProjectLogEntity> logs = logRepository.findByProjectIdOrderByTimestampDesc(project.getId());

        List<Map<String, Object>> response = new ArrayList<>();
        for (ProjectLogEntity l : logs) {
            Map<String, Object> lm = new LinkedHashMap<>();
            lm.put("id", l.getId());
            lm.put("level", l.getLogLevel());
            lm.put("source", l.getSource());
            lm.put("message", l.getMessage());
            lm.put("timestamp", l.getTimestamp().toString());
            response.add(lm);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "Get version history for project")
    public ResponseEntity<List<Map<String, Object>>> getProjectVersions(@PathVariable UUID id, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectEntity project = projectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));

        List<ProjectVersionEntity> versions = versionRepository.findByProjectIdOrderByVersionNumberDesc(project.getId());

        List<Map<String, Object>> response = new ArrayList<>();
        for (ProjectVersionEntity v : versions) {
            Map<String, Object> vm = new LinkedHashMap<>();
            vm.put("id", v.getPublicId().toString());
            vm.put("versionNumber", v.getVersionNumber());
            vm.put("changeSummary", v.getChangeSummary() != null ? v.getChangeSummary() : "");
            vm.put("status", v.getStatus());
            vm.put("createdAt", v.getCreatedAt().toString());
            response.add(vm);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/stop")
    @Operation(summary = "Stop running project process")
    public ResponseEntity<Map<String, Object>> stopProject(@PathVariable UUID id, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectEntity project = projectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));

        project.setStatus("STOPPED");
        project.setRuntimeStatus("STOPPED");
        projectRepository.save(project);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("projectId", id.toString());
        res.put("runtimeStatus", "STOPPED");
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user project")
    public ResponseEntity<Map<String, Object>> deleteProject(@PathVariable UUID id, Authentication authentication) {
        UserEntity user = getAuthenticatedUser(authentication);
        ProjectEntity project = projectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));

        project.setDeletedAt(java.time.OffsetDateTime.now());
        projectRepository.save(project);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("projectId", id.toString());
        res.put("deleted", true);
        return ResponseEntity.ok(res);
    }

    private Map<String, Object> mapProjectSummary(ProjectEntity p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getPublicId().toString());
        map.put("name", p.getName());
        map.put("description", p.getDescription() != null ? p.getDescription() : "");
        map.put("status", p.getStatus());
        map.put("progress", p.getProgress() != null ? p.getProgress() : 0);
        map.put("runtimeStatus", p.getRuntimeStatus() != null ? p.getRuntimeStatus() : "STOPPED");
        map.put("activeTechStackJson", p.getActiveTechStackJson() != null ? p.getActiveTechStackJson() : "{}");
        map.put("previewUrl", p.getPreviewUrl() != null ? p.getPreviewUrl() : "");
        map.put("updatedAt", p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : "");
        return map;
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
