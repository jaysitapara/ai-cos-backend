package com.app.model.planner;

import com.app.enums.TaskState;
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
public class TaskItem {
    private String taskId;
    private UUID executionId;
    private String name;
    private String description;
    private String objective;
    private int priority;
    private TaskState status;
    private String assignedAgentId;
    private String parentTaskId;
    private List<String> dependencies;
    private Map<String, Object> inputs;
    private Map<String, Object> outputs;
    private int retryCount;
    private Instant createdAt;
    private Instant updatedAt;
}
