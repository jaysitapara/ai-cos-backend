package com.app.model.orchestrator;

import com.app.enums.ExecutionState;
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
public class ExecutionContext {
    private UUID executionId;
    private UUID userId;
    private UUID workspaceId;
    private UUID conversationId;
    private String goalPrompt;
    private List<String> attachmentIds;
    private List<String> assumptions;
    private Map<String, Object> configuration;
    private ExecutionState status;
    private Instant createdAt;
    private Instant updatedAt;
}
