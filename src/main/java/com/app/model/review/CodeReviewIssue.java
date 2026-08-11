package com.app.model.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeReviewIssue {
    private String issueId;
    private String description;
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW, INFORMATIONAL
    private String location;
    private String rootCause;
    private String suggestedResolution;
}
