package com.app.model.workflow;

import com.app.enums.WorkflowState;
import com.app.model.planner.TaskItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowModel {
    private String workflowId;
    private UUID executionId;
    private String name;
    private String description;
    private String version;
    private WorkflowState state;
    private List<TaskItem> tasks;
    private List<Map<String, Object>> dependencies;
    private List<Map<String, Object>> branches;
    private List<ApprovalGate> approvalGates;
    private Instant createdAt;
    private Instant updatedAt;
}
