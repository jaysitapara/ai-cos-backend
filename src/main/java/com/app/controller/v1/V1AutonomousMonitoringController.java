package com.app.controller.v1;

import com.app.common.ApiConstants;
import com.app.model.monitoring.NotificationModel;
import com.app.model.monitoring.RollbackPlanModel;
import com.app.service.AutonomousMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/autonomous-monitoring")
@RequiredArgsConstructor
@Tag(name = "V1 Autonomous Monitoring Engine", description = "Monitoring, Notifications & Rollback REST APIs")
public class V1AutonomousMonitoringController {

    private final AutonomousMonitoringService monitoringService;

    @GetMapping("/notifications")
    @Operation(summary = "List system notifications and alerts")
    public ResponseEntity<List<NotificationModel>> getNotifications() {
        return ResponseEntity.ok(monitoringService.getNotifications());
    }

    @PostMapping("/notifications/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<NotificationModel> markRead(@PathVariable String id) {
        return ResponseEntity.ok(monitoringService.markRead(id));
    }

    @PostMapping("/rollback/trigger")
    @Operation(summary = "Trigger automated deployment or configuration rollback")
    public ResponseEntity<RollbackPlanModel> triggerRollback(@RequestBody Map<String, String> body) {
        String deploymentId = body.getOrDefault("deploymentId", "dep-default");
        String type = body.getOrDefault("rollbackType", "FULL");
        return ResponseEntity.ok(monitoringService.triggerRollback(deploymentId, type));
    }

    @GetMapping("/rollback/{id}")
    @Operation(summary = "Get rollback execution details by ID")
    public ResponseEntity<RollbackPlanModel> getRollback(@PathVariable String id) {
        return monitoringService.getRollback(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
