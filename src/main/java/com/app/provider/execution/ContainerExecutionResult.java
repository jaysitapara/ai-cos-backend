package com.app.provider.execution;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ContainerExecutionResult {
    private String containerId;
    private Integer runtimePort;
    private int exitCode;
    private List<String> logs;
    private boolean isSuccessful;
    private String errorMessage;
}
