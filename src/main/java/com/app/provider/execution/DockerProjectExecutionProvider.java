package com.app.provider.execution;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component("dockerProjectExecutionProvider")
public class DockerProjectExecutionProvider implements ProjectExecutionProvider {

    @Value("${app.execution.mode:container}")
    private String executionMode;

    @Value("${app.execution.container.image.node:node:22-alpine}")
    private String nodeImage;

    @Value("${app.execution.container.image.java:eclipse-temurin:21-jdk-alpine}")
    private String javaImage;

    @Value("${app.execution.container.cpu-limit:1.0}")
    private String cpuLimit;

    @Value("${app.execution.container.memory-limit:512m}")
    private String memoryLimit;

    @Value("${app.execution.container.pids-limit:128}")
    private String pidsLimit;

    @Override
    public boolean isAvailable() {
        try {
            Process process = new ProcessBuilder("docker", "info").start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            log.warn("Docker daemon check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "DOCKER_CONTAINER";
    }

    @Override
    public ContainerExecutionResult executeCommand(ProjectExecutionRequest request) {
        String containerName = "ai-cos-project-" + request.getProjectId() + "-" + (request.getJobId() != null ? request.getJobId() : System.currentTimeMillis());
        int hostPort = request.getRequestedPort() != null ? request.getRequestedPort() : (41000 + (int) (request.getProjectId() % 8000));

        if (!isAvailable()) {
            log.warn("Docker daemon is not available locally for execution request on project ID {}", request.getProjectId());
            return ContainerExecutionResult.builder()
                .containerId(containerName)
                .runtimePort(hostPort)
                .exitCode(1)
                .logs(List.of("Docker daemon is not accessible locally."))
                .isSuccessful(false)
                .errorMessage("Docker daemon is not accessible locally.")
                .build();
        }

        Path absoluteWorkspace = request.getProjectDir().toAbsolutePath().normalize();

        List<String> dockerCmd = new ArrayList<>();
        dockerCmd.add("docker");
        dockerCmd.add("run");

        if ("RUNTIME".equalsIgnoreCase(request.getCommandType())) {
            dockerCmd.add("-d"); // Run in background for runtime dev server
        } else {
            dockerCmd.add("--rm"); // Clean up container on finish for install/build
        }

        dockerCmd.add("--name");
        dockerCmd.add(containerName);

        // Security Hardening & Resource Isolation
        dockerCmd.add("--user");
        dockerCmd.add("1000:1000"); // Non-root execution user

        dockerCmd.add("--cap-drop");
        dockerCmd.add("ALL");

        dockerCmd.add("--security-opt");
        dockerCmd.add("no-new-privileges:true");

        dockerCmd.add("--cpus");
        dockerCmd.add(cpuLimit);

        dockerCmd.add("--memory");
        dockerCmd.add(memoryLimit);

        dockerCmd.add("--pids-limit");
        dockerCmd.add(pidsLimit);

        // Labels for orphan tracking
        dockerCmd.add("--label");
        dockerCmd.add("ai_cos=true");
        dockerCmd.add("--label");
        dockerCmd.add("project_id=" + request.getProjectId());
        dockerCmd.add("--label");
        dockerCmd.add("user_id=" + request.getUserId());

        // Environment Isolation (Pass ONLY safe runtime variables, NEVER application secrets!)
        dockerCmd.add("-e");
        dockerCmd.add("PORT=3000");
        dockerCmd.add("-e");
        dockerCmd.add("NODE_ENV=development");

        // Port Mapping
        dockerCmd.add("-p");
        dockerCmd.add(hostPort + ":3000");

        // Volume Mounting (Project directory mounted into container /app)
        dockerCmd.add("-v");
        dockerCmd.add(absoluteWorkspace.toString() + ":/app");

        dockerCmd.add("-w");
        dockerCmd.add("/app");

        String image = "SPRING_BOOT".equalsIgnoreCase(request.getTechStack()) ? javaImage : nodeImage;
        dockerCmd.add(image);

        // Allowlisted Commands
        if ("INSTALL".equalsIgnoreCase(request.getCommandType())) {
            dockerCmd.add("npm");
            dockerCmd.add("install");
        } else if ("BUILD".equalsIgnoreCase(request.getCommandType())) {
            dockerCmd.add("npm");
            dockerCmd.add("run");
            dockerCmd.add("build");
        } else {
            dockerCmd.add("npm");
            dockerCmd.add("run");
            dockerCmd.add("dev");
        }

        log.info("Executing isolated container command: {}", String.join(" ", dockerCmd));

        List<String> logs = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(dockerCmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            int timeout = request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : 180;
            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                stopAndRemoveContainer(containerName);
                return ContainerExecutionResult.builder()
                    .containerId(containerName)
                    .runtimePort(hostPort)
                    .exitCode(-1)
                    .logs(List.of("Execution timed out after " + timeout + " seconds."))
                    .isSuccessful(false)
                    .errorMessage("Execution timed out after " + timeout + " seconds.")
                    .build();
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logs.add(line);
                }
            }

            boolean isSuccess = process.exitValue() == 0 || "RUNTIME".equalsIgnoreCase(request.getCommandType());
            return ContainerExecutionResult.builder()
                .containerId(containerName)
                .runtimePort(hostPort)
                .exitCode(process.exitValue())
                .logs(logs)
                .isSuccessful(isSuccess)
                .build();

        } catch (Exception e) {
            log.error("Failed to execute container command: {}", e.getMessage(), e);
            stopAndRemoveContainer(containerName);
            return ContainerExecutionResult.builder()
                .containerId(containerName)
                .runtimePort(hostPort)
                .exitCode(1)
                .logs(List.of("Execution failure: " + e.getMessage()))
                .isSuccessful(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }

    @Override
    public void stopAndRemoveContainer(String containerId) {
        if (containerId == null || containerId.isBlank()) return;
        try {
            log.info("Stopping and removing container [{}]", containerId);
            new ProcessBuilder("docker", "stop", containerId).start().waitFor(5, TimeUnit.SECONDS);
            new ProcessBuilder("docker", "rm", "-f", containerId).start().waitFor(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to stop/remove container [{}]: {}", containerId, e.getMessage());
        }
    }

    @Override
    public List<String> getContainerLogs(String containerId, int maxLines) {
        if (containerId == null || containerId.isBlank()) return Collections.emptyList();
        List<String> logs = new ArrayList<>();
        try {
            Process process = new ProcessBuilder("docker", "logs", "--tail", String.valueOf(maxLines), containerId).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logs.add(line);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to read container logs for [{}]: {}", containerId, e.getMessage());
        }
        return logs;
    }
}
