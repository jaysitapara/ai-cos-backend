package com.app.provider.execution;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.util.Map;

@Data
@Builder
public class ProjectExecutionRequest {
    private Long projectId;
    private Long userId;
    private Long jobId;
    private Path projectDir;
    private String techStack; // REACT, SPRING_BOOT
    private String commandType; // INSTALL, BUILD, RUNTIME
    private Map<String, String> environmentMap;
    private Integer timeoutSeconds;
    private Integer requestedPort;
}
