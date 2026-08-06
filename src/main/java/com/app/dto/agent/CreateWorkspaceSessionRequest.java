package com.app.dto.agent;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkspaceSessionRequest {

    @NotBlank(message = "Goal prompt cannot be blank")
    private String goalPrompt;

    private List<UploadedFileDTO> uploadedFiles;
}
