package com.app.controller.v1;

import com.app.common.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/attachments")
@RequiredArgsConstructor
@Tag(name = "V1 Attachments", description = "File Upload & Attachment REST APIs")
public class V1AttachmentController {

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload supporting attachment file")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        String id = UUID.randomUUID().toString();
        return ResponseEntity.ok(Map.of(
            "id", id,
            "fileName", file.getOriginalFilename() != null ? file.getOriginalFilename() : "file",
            "sizeBytes", file.getSize(),
            "mimeType", file.getContentType() != null ? file.getContentType() : "application/octet-stream",
            "status", "COMPLETED",
            "processingStatus", "INDEXED",
            "uploadedAt", System.currentTimeMillis()
        ));
    }

    @GetMapping
    @Operation(summary = "List all user attachments")
    public ResponseEntity<List<Map<String, Object>>> listAttachments() {
        return ResponseEntity.ok(List.of());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove an attachment by ID")
    public ResponseEntity<Void> removeAttachment(@PathVariable String id) {
        return ResponseEntity.noContent().build();
    }
}
