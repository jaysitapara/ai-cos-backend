package com.app.model.planner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionPlanModel {
    private String planId;
    private UUID executionId;
    private String goalPrompt;
    private String summary;
    private List<TaskItem> tasks;
    private int totalEstimatedComplexity;
    private Instant createdAt;
}
