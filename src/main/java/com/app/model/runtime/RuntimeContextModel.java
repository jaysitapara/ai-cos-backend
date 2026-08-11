package com.app.model.runtime;

import com.app.enums.RuntimeState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuntimeContextModel {
    private String runtimeId;
    private UUID executionId;
    private String planId;
    private String workflowId;
    private RuntimeState status;
    private double overallProgressPercent;
    private int completedTasksCount;
    private int totalTasksCount;
    private String activeTaskName;
    private Instant createdAt;
    private Instant updatedAt;
}
