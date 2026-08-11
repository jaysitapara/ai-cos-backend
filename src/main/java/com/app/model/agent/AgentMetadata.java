package com.app.model.agent;

import com.app.enums.AgentCategory;
import com.app.enums.AgentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMetadata {
    private String agentId;
    private String name;
    private String description;
    private String version;
    private AgentCategory category;
    private List<String> capabilities;
    private List<String> inputTypes;
    private List<String> outputTypes;
    private List<String> requiredPermissions;
    private AgentStatus status;
    private AgentHealth health;
    private int priority;
}
