package com.app.dto.agent;

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
    private String currentPhase;
    private String currentAgentRole;
    private int totalTasks;
    private int completedTasks;
    private int runningTasks;
    private int waitingTasks;
    private int failedTasks;
    private int progressPercentage;
    private String estimatedTimeRemaining;
    private String currentReasoningSummary;
    private List<AgentTaskDTO> tasks;
    private List<AgentLogDTO> logs;
}
