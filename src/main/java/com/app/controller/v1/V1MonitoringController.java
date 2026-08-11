package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.monitoring.SystemHealthReport;
import com.app.model.orchestrator.ExecutionContext;
import com.app.service.RecoveryEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/monitoring")
@RequiredArgsConstructor
@Tag(name = "V1 Monitoring", description = "Observability, Health & Recovery Engine REST APIs")
public class V1MonitoringController {

    private final RecoveryEngineService recoveryEngineService;

    @GetMapping("/health")
    @Operation(summary = "Get system, workflow and agent health report")
    public ResponseEntity<SystemHealthReport> getHealthReport() {
        return ResponseEntity.ok(recoveryEngineService.generateHealthReport());
    }

    @GetMapping("/diagnostics")
    @Operation(summary = "Get operational metrics and diagnostics")
    public ResponseEntity<Map<String, Object>> getDiagnostics() {
        SystemHealthReport report = recoveryEngineService.generateHealthReport();
        return ResponseEntity.ok(Map.of(
            "overallStatus", report.getOverallStatus(),
            "activeExecutions", report.getActiveExecutionsCount(),
            "queueDepth", report.getQueueDepth(),
            "successRatePercent", report.getSuccessRatePercent(),
            "metrics", report.getMetrics()
        ));
    }

    @PostMapping("/recover/{executionId}")
    @Operation(summary = "Trigger recovery action for a failed or stalled execution")
    public ResponseEntity<ExecutionContext> recoverExecution(@PathVariable UUID executionId) {
        return ResponseEntity.ok(recoveryEngineService.recoverExecution(executionId));
    }
}
