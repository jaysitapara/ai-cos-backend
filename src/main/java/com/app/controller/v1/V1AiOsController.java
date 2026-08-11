package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.aios.AiCapability;
import com.app.model.aios.AiOsSystemStatus;
import com.app.service.AiOperatingSystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/ai-os")
@RequiredArgsConstructor
@Tag(name = "V1 AI Operating System Engine", description = "AI Operating System Core Architecture REST APIs")
public class V1AiOsController {

    private final AiOperatingSystemService aiOperatingSystemService;

    @GetMapping("/status")
    @Operation(summary = "Get AI Operating System status and runtime metrics")
    public ResponseEntity<AiOsSystemStatus> getStatus() {
        return ResponseEntity.ok(aiOperatingSystemService.getSystemStatus());
    }

    @GetMapping("/capabilities")
    @Operation(summary = "List registered AI OS capabilities and engine modules")
    public ResponseEntity<List<AiCapability>> getCapabilities() {
        return ResponseEntity.ok(aiOperatingSystemService.getCapabilities());
    }

    @GetMapping("/config")
    @Operation(summary = "Get AI OS system environment configuration")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(aiOperatingSystemService.getSystemConfig());
    }
}
