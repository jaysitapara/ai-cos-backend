package com.app.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OrganizationResponse(
    UUID publicId,
    String name,
    String slug,
    String planType,
    List<WorkspaceResponse> workspaces
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record WorkspaceResponse(
        UUID publicId,
        String name,
        String slug
    ) {}
}
