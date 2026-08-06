package com.app.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactResponse {
    private UUID publicId;
    private String filePath;
    private String fileName;
    private String artifactType;
    private String content;
    private String agentRole;
    private OffsetDateTime createdAt;
}
