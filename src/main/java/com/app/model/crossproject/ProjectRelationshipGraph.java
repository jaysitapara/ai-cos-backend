package com.app.model.crossproject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRelationshipGraph {
    private String projectId;
    private List<String> relatedProjectIds;
    private int sharedComponentsCount;
    private int sharedApisCount;
    private List<String> sharedTechnologies;
}
