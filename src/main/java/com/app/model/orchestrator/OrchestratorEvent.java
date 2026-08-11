package com.app.model.orchestrator;

import com.app.enums.ExecutionState;
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
public class OrchestratorEvent {
    private String eventId;
    private UUID executionId;
    private String eventType; // e.g. ExecutionCreated, PlanningStarted, PlanningCompleted, ApprovalRequested, ExecutionStarted, ProgressUpdated, ExecutionCompleted, ExecutionFailed
    private ExecutionState currentState;
    private String message;
    private Instant timestamp;
}
