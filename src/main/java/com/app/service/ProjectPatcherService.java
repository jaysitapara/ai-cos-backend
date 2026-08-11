package com.app.service;

import com.app.entity.*;
import com.app.exception.ResourceNotFoundException;
import com.app.exception.UnauthorizedException;
import com.app.provider.ai.AiCompletionRequest;
import com.app.provider.ai.AiCompletionResponse;
import com.app.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectPatcherService {

    private final ProjectRepository projectRepository;
    private final ProjectVersionRepository versionRepository;
    private final ProjectFileRepository fileRepository;
    private final ProjectCommentRepository commentRepository;
    private final ProjectJobRepository jobRepository;
    private final ProjectLogRepository logRepository;
    private final AIService aiService;
    private final ObjectMapper objectMapper;
    private final com.app.provider.execution.ContainerExecutionService containerExecutionService;

    private static final String WORKSPACE_ROOT = "workspace/projects";

    @Async
    public CompletableFuture<Void> processLiveUpdateAsync(Long projectId, Long commentId, Long userId) {
        log.info("Processing Live Update for project ID {}, comment ID {}", projectId, commentId);

        ProjectEntity project = projectRepository.findById(projectId).orElse(null);
        ProjectCommentEntity comment = commentRepository.findById(commentId).orElse(null);

        if (project == null || comment == null) return CompletableFuture.completedFuture(null);

        project.setStatus("UPDATING");
        project.setRuntimeStatus("UPDATING");
        projectRepository.save(project);

        ProjectJobEntity job = ProjectJobEntity.builder()
                .publicId(UUID.randomUUID())
                .project(project)
                .jobType("PROJECT_UPDATE")
                .status("IN_PROGRESS")
                .startedAt(OffsetDateTime.now())
                .build();
        job = jobRepository.save(job);

        addLog(project, job, "INFO", "UPDATE", "Live Update requested: \"" + comment.getCommentText() + "\"");

        try {
            // 1. Get latest project version and current files
            ProjectVersionEntity latestVersion = versionRepository.findFirstByProjectIdOrderByVersionNumberDesc(projectId).orElse(null);
            int nextVersionNum = (latestVersion != null ? latestVersion.getVersionNumber() : 1) + 1;

            List<ProjectFileEntity> currentFiles = fileRepository.findByProjectIdAndIsDeletedFalseOrderByFilePathAsc(projectId);

            // 2. Target file selection: retrieve relevant files rather than sending full project
            ProjectFileEntity targetFile = selectTargetFile(currentFiles, comment.getFilePath(), comment.getCommentText());

            // 3. AI Incremental Patch Generation
            String updatedContent = generateIncrementalPatch(project, targetFile, comment.getCommentText());

            // 4. Save new Project Version
            ProjectVersionEntity newVersion = ProjectVersionEntity.builder()
                    .publicId(UUID.randomUUID())
                    .project(project)
                    .versionNumber(nextVersionNum)
                    .parentVersionId(latestVersion != null ? latestVersion.getId() : null)
                    .changeSummary("Live Update: " + comment.getCommentText())
                    .userCommentId(comment.getId())
                    .changedFilesJson(objectMapper.writeValueAsString(List.of(targetFile.getFilePath())))
                    .status("COMPLETED")
                    .createdAt(OffsetDateTime.now())
                    .build();
            newVersion = versionRepository.save(newVersion);

            // 5. Update target file in database
            targetFile.setContent(updatedContent);
            targetFile.setVersion(newVersion);
            targetFile.setUpdatedAt(OffsetDateTime.now());
            fileRepository.save(targetFile);

            // 6. Write patched file to disk workspace
            Path projectDir = writePatchedFileToDisk(project, targetFile);

            // 7. Validate build in isolated container
            addLog(project, job, "INFO", "VALIDATION", "Validating patched version " + nextVersionNum + " in isolated container...");
            var buildReq = com.app.provider.execution.ProjectExecutionRequest.builder()
                .projectId(project.getId())
                .userId(project.getUser().getId())
                .jobId(job.getId())
                .projectDir(projectDir)
                .techStack("REACT")
                .commandType("BUILD")
                .timeoutSeconds(180)
                .build();

            var buildResult = containerExecutionService.executeProjectStep(buildReq);
            if (!buildResult.isSuccessful()) {
                throw new RuntimeException("Patch build validation failed: " + buildResult.getErrorMessage());
            }

            // 8. Update Project status to RUNNING
            project.setCurrentVersionId(newVersion.getId());
            project.setStatus("RUNNING");
            project.setRuntimeStatus("RUNNING");
            project.setLastRunAt(OffsetDateTime.now());
            projectRepository.save(project);

            comment.setStatus("APPLIED");
            commentRepository.save(comment);

            job.setStatus("COMPLETED");
            job.setVersion(newVersion);
            job.setCompletedAt(OffsetDateTime.now());
            jobRepository.save(job);

            addLog(project, job, "INFO", "UPDATE", "Live Update applied cleanly. Verified Version " + nextVersionNum + " in container.");

        } catch (Exception e) {
            log.error("Live Update failed for project {}: {}", projectId, e.getMessage(), e);
            project.setStatus("FAILED");
            project.setRuntimeStatus("FAILED");
            projectRepository.save(project);

            comment.setStatus("FAILED");
            commentRepository.save(comment);

            job.setStatus("FAILED");
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(OffsetDateTime.now());
            jobRepository.save(job);

            addLog(project, job, "ERROR", "UPDATE", "Live Update failed: " + e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    private ProjectFileEntity selectTargetFile(List<ProjectFileEntity> files, String requestedFilePath, String commentText) {
        if (requestedFilePath != null && !requestedFilePath.isBlank()) {
            for (ProjectFileEntity f : files) {
                if (f.getFilePath().equalsIgnoreCase(requestedFilePath)) {
                    return f;
                }
            }
        }

        // Fallback heuristic: find main component file src/App.tsx or first .tsx / .js file
        for (ProjectFileEntity f : files) {
            if ("src/App.tsx".equalsIgnoreCase(f.getFilePath())) {
                return f;
            }
        }
        for (ProjectFileEntity f : files) {
            if (f.getFilePath().endsWith(".tsx") || f.getFilePath().endsWith(".jsx") || f.getFilePath().endsWith(".ts")) {
                return f;
            }
        }
        return files.get(0);
    }

    private String generateIncrementalPatch(ProjectEntity project, ProjectFileEntity file, String changeRequest) {
        String systemInstruction = "You are a expert code patcher. Perform an incremental update to the provided file content based strictly on the change request. Return ONLY the complete updated file content without markdown code block fences.";

        String prompt = "Target File: " + file.getFilePath() + "\n\n" +
                "Current File Content:\n" + file.getContent() + "\n\n" +
                "User Change Request: " + changeRequest + "\n\n" +
                "Updated File Content:";

        AiCompletionRequest request = new AiCompletionRequest(prompt, systemInstruction, null, 0.2, 2048, Map.of());
        AiCompletionResponse response = aiService.generateCompletion(request, project.getUser(), project.getId(), null, "PATCH", "CODE_UPDATER");

        String content = response != null ? response.getContent() : file.getContent();
        if (content.startsWith("```")) {
            int firstNewline = content.indexOf('\n');
            if (firstNewline != -1 && content.endsWith("```")) {
                content = content.substring(firstNewline + 1, content.length() - 3).trim();
            }
        }
        return content;
    }

    private Path writePatchedFileToDisk(ProjectEntity project, ProjectFileEntity file) {
        try {
            Path projectPath = Paths.get(WORKSPACE_ROOT, project.getUser().getId().toString(), project.getPublicId().toString());
            Path filePath = projectPath.resolve(file.getFilePath());
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, file.getContent());
            return projectPath;
        } catch (Exception e) {
            log.error("Failed to write patched file to disk: {}", e.getMessage(), e);
            return Paths.get(WORKSPACE_ROOT, project.getUser().getId().toString(), project.getPublicId().toString());
        }
    }

    private void addLog(ProjectEntity project, ProjectJobEntity job, String level, String source, String message) {
        ProjectLogEntity l = ProjectLogEntity.builder()
                .project(project)
                .job(job)
                .logLevel(level)
                .source(source)
                .message(message)
                .timestamp(OffsetDateTime.now())
                .build();
        logRepository.save(l);
    }
}
