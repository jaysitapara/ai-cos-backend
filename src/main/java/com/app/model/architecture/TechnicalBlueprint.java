package com.app.model.architecture;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalBlueprint {
    private String blueprintId;
    private String specId;
    private String systemArchitectureMarkdown;
    private List<EntitySchema> entities;
    private List<ApiEndpointSpec> apiEndpoints;
    private String deploymentTopology;
    private Instant createdAt;
}
