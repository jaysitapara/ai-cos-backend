package com.app.model.requirement;

import com.app.enums.RequirementCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementItem {
    private String requirementId;
    private String analysisId;
    private RequirementCategory category;
    private String title;
    private String description;
    private String priority; // HIGH, MEDIUM, LOW
    private String source; // PROMPT, ATTACHMENT, INFERRED
    private double confidenceScore;
    private List<String> dependencies;
    private List<String> assumptions;
    private String status; // IDENTIFIED, VERIFIED, MODIFIED
}
