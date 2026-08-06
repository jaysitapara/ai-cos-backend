package com.app.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadedFileDTO {
    private String fileName;
    private String fileType;
    private long sizeBytes;
    private String extractedSummary;
}
