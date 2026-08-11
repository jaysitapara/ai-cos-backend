package com.app.provider.execution;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ContainerExecutionService {

    private final DockerProjectExecutionProvider dockerProvider;
    private final HostProjectExecutionProvider hostProvider;
    private final Environment environment;

    @Value("${app.execution.mode:${PROJECT_EXECUTION_MODE:container}}")
    private String executionMode;

    @Autowired
    public ContainerExecutionService(DockerProjectExecutionProvider dockerProvider,
                                     HostProjectExecutionProvider hostProvider,
                                     @Autowired(required = false) Environment environment) {
        this.dockerProvider = dockerProvider;
        this.hostProvider = hostProvider;
        this.environment = environment;
    }

    public ProjectExecutionProvider getActiveProvider() {
        if ("host".equalsIgnoreCase(executionMode)) {
            log.warn("Active execution provider: HOST (Development mode only — UNRESTRICTED PROCESS EXECUTION)");
            return hostProvider;
        }

        if (dockerProvider != null && dockerProvider.isAvailable()) {
            return dockerProvider;
        }

        // Docker is NOT available on this system.
        boolean isProduction = isProductionEnvironment();

        if (isProduction) {
            log.error("CRITICAL PRODUCTION SECURITY ERROR: Isolated container execution is required in production (app.execution.mode=container), but Docker daemon is unavailable!");
            throw new IllegalStateException("Isolated project execution is currently unavailable (Docker daemon not accessible). Host process fallback is disabled in production.");
        }

        // In local development mode, safely fall back to host process execution when Docker is unavailable
        log.warn("Docker daemon is not running locally. Safely falling back to HostProjectExecutionProvider for local development.");
        return hostProvider;
    }

    public ContainerExecutionResult executeProjectStep(ProjectExecutionRequest request) {
        ProjectExecutionProvider provider = getActiveProvider();
        log.info("Executing project step [{}] for project [{}] via provider [{}]", request.getCommandType(), request.getProjectId(), provider.getProviderName());
        return provider.executeCommand(request);
    }

    public void stopAndRemoveContainer(String containerId) {
        if (containerId == null || containerId.isBlank()) return;
        try {
            if (dockerProvider != null && dockerProvider.isAvailable()) {
                dockerProvider.stopAndRemoveContainer(containerId);
            }
        } catch (Exception e) {
            log.warn("Failed to stop container [{}]: {}", containerId, e.getMessage());
        }
    }

    public List<String> getContainerLogs(String containerId, int maxLines) {
        if (containerId == null || containerId.isBlank()) return List.of();
        try {
            if (dockerProvider != null && dockerProvider.isAvailable()) {
                return dockerProvider.getContainerLogs(containerId, maxLines);
            }
        } catch (Exception e) {
            log.warn("Failed to read container logs: {}", e.getMessage());
        }
        return List.of();
    }

    public boolean isProductionEnvironment() {
        if (environment == null) {
            return false;
        }
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles != null) {
            for (String profile : activeProfiles) {
                if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                    return true;
                }
            }
        }
        String envProp = System.getProperty("env", System.getenv("ENV"));
        if ("prod".equalsIgnoreCase(envProp) || "production".equalsIgnoreCase(envProp)) {
            return true;
        }
        String springEnv = System.getProperty("spring.profiles.active", System.getenv("SPRING_PROFILES_ACTIVE"));
        return "prod".equalsIgnoreCase(springEnv) || "production".equalsIgnoreCase(springEnv);
    }
}
