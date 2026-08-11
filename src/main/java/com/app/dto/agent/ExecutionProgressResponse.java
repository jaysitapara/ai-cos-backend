package com.app.dto.agent;

import com.app.entity.ExecutionMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionProgressResponse {
    private String sessionStatus;
    private ExecutionMode executionMode;
    private String startedAt;
    private long elapsedTimeSeconds;
    private String currentPhase;
    private String currentAgentRole;
    private String currentAgentName;
    private String currentTaskTitle;
    private String currentFileName;
    private int totalTasks;
    private int completedTasks;
    private int runningTasks;
    private int waitingTasks;
    private int failedTasks;
    private int skippedTasks;
    private int totalAgents;
    private int completedAgents;
    private int progressPercentage;
    private String estimatedTimeRemaining;
    private String currentReasoningSummary;
    private long promptTokens;
    private long completionTokens;
    private long totalTokens;
    private double estimatedCost;
    private List<AgentTaskDTO> tasks;
    private List<AgentLogDTO> logs;
    private List<ArtifactResponse> artifacts;
}
