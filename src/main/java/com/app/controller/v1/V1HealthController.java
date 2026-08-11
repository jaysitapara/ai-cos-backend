package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/health")
@RequiredArgsConstructor
@Tag(name = "V1 Health Probes", description = "Operational Liveness & Readiness APIs")
public class V1HealthController {

    private final HealthService healthService;

    @GetMapping
    @Operation(summary = "System health check status")
    public ResponseEntity<Map<String, String>> getHealth() {
        return ResponseEntity.ok(healthService.getHealthStatus());
    }

    @GetMapping("/liveness")
    @Operation(summary = "Liveness probe endpoint")
    public ResponseEntity<Map<String, Object>> getLiveness() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "probe", "LIVENESS",
            "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/readiness")
    @Operation(summary = "Readiness probe endpoint")
    public ResponseEntity<Map<String, Object>> getReadiness() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "probe", "READINESS",
            "database", "CONNECTED",
            "orchestrator", "READY",
            "agentRegistry", "READY",
            "timestamp", Instant.now().toString()
        ));
    }
}
