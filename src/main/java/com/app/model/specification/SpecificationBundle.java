package com.app.model.specification;

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
public class SpecificationBundle {
    private String specId;
    private String analysisId;
    private String brdContent;
    private String prdContent;
    private List<UserStoryItem> userStories;
    private Map<String, Object> functionalSpecs;
    private Instant createdAt;
}
