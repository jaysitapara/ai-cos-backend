package com.app.model.requirement;

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
public class RequirementAnalysisResult {
    private String analysisId;
    private String goalPrompt;
    private String projectType; // WEB_APP, MICROSERVICE, CLI, FULL_STACK
    private String estimatedComplexity; // LOW, MEDIUM, HIGH
    private List<RequirementItem> requirements;
    private List<String> assumptions;
    private List<String> detectedAmbiguities;
    private Instant createdAt;
}
