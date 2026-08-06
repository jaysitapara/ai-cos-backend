package com.app.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTaskDTO {
    private Long id;
    private String taskKey;
    private String title;
    private String agentRole;
    private String phase;
    private Integer executionOrder;
    private List<String> dependencies;
    private String status;
    private Integer retries;
    private String reasoningSummary;
    private String outputSummary;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
}
