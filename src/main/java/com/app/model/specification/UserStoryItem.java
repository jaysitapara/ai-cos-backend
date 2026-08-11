package com.app.model.specification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStoryItem {
    private String storyId;
    private String epic;
    private String persona;
    private String title;
    private String userStory;
    private List<String> acceptanceCriteria;
    private String priority;
    private List<String> dependencies;
    private String estimatedComplexity;
    private String status;
}
