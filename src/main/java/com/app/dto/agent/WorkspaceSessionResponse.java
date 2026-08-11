package com.app.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceSessionResponse {
    private UUID publicId;
    private String goalPrompt;
    private String status;
    private String projectType;
    private String complexity;
    private String estimatedScope;
    private List<UploadedFileDTO> uploadedFiles;
    private List<String> assumptions;
    private List<String> missingInfo;
    private ImplementationPlanResponse plan;
    private com.app.entity.ExecutionMode executionMode;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
