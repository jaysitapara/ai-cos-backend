package com.app.model.backendgen;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackendPlan {
    private String planId;
    private String blueprintId;
    private List<String> featureModules;
    private Map<String, Object> testingStrategy;
    private Map<String, String> documentationArtifacts;
    private Instant createdAt;
}
