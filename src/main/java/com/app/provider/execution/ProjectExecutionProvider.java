package com.app.provider.execution;

import java.util.List;

public interface ProjectExecutionProvider {
    boolean isAvailable();
    String getProviderName();
    ContainerExecutionResult executeCommand(ProjectExecutionRequest request);
    void stopAndRemoveContainer(String containerId);
    List<String> getContainerLogs(String containerId, int maxLines);
}
