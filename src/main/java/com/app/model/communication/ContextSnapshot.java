package com.app.model.communication;

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
public class ContextSnapshot {
    private String snapshotId;
    private UUID executionId;
    private int version;
    private String goalPrompt;
    private List<String> requirements;
    private List<String> assumptions;
    private List<String> constraints;
    private ExecutionState status;
    private Instant timestamp;
}
