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
@Component("hostProjectExecutionProvider")
public class HostProjectExecutionProvider implements ProjectExecutionProvider {

    @Value("${app.execution.mode:container}")
    private String executionMode;

    @Override
    public boolean isAvailable() {
        return "host".equalsIgnoreCase(executionMode);
    }

    @Override
    public String getProviderName() {
        return "HOST_PROCESS_DEV_ONLY";
    }

    @Override
    public ContainerExecutionResult executeCommand(ProjectExecutionRequest request) {
        log.warn("SECURITY WARNING: Host process execution is active for project ID {}. HOST MODE IS NOT SAFE FOR MULTI-TENANT PRODUCTION!", request.getProjectId());

        Path absoluteWorkspace = request.getProjectDir().toAbsolutePath().normalize();
        int hostPort = request.getRequestedPort() != null ? request.getRequestedPort() : (41000 + (int) (request.getProjectId() % 8000));

        List<String> cmd = new ArrayList<>();
        if ("INSTALL".equalsIgnoreCase(request.getCommandType())) {
            cmd.add("npm");
            cmd.add("install");
        } else if ("BUILD".equalsIgnoreCase(request.getCommandType())) {
            cmd.add("npm");
            cmd.add("run");
            cmd.add("build");
        } else {
            cmd.add("npm");
            cmd.add("run");
            cmd.add("dev");
        }

        List<String> logs = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(absoluteWorkspace.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            int timeout = request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : 180;
            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return ContainerExecutionResult.builder()
                    .runtimePort(hostPort)
                    .exitCode(-1)
                    .logs(List.of("Execution timed out."))
                    .isSuccessful(false)
                    .errorMessage("Timed out.")
                    .build();
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logs.add(line);
                }
            }

            return ContainerExecutionResult.builder()
                .runtimePort(hostPort)
                .exitCode(process.exitValue())
                .logs(logs)
                .isSuccessful(process.exitValue() == 0)
                .build();

        } catch (Exception e) {
            log.error("Host process execution failed: {}", e.getMessage(), e);
            return ContainerExecutionResult.builder()
                .runtimePort(hostPort)
                .exitCode(1)
                .logs(List.of("Failure: " + e.getMessage()))
                .isSuccessful(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }

    @Override
    public void stopAndRemoveContainer(String containerId) {
        log.info("Stop requested for host process [{}]", containerId);
    }

    @Override
    public List<String> getContainerLogs(String containerId, int maxLines) {
        return Collections.emptyList();
    }
}
