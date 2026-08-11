package com.app.service;

import com.app.model.monitoring.NotificationModel;
import com.app.model.monitoring.RollbackPlanModel;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AutonomousMonitoringService {

    private final Map<String, NotificationModel> notificationStore = new ConcurrentHashMap<>();
    private final Map<String, RollbackPlanModel> rollbackStore = new ConcurrentHashMap<>();

    public List<NotificationModel> getNotifications() {
        if (notificationStore.isEmpty()) {
            NotificationModel notif = NotificationModel.builder()
                .notificationId("notif-1")
                .type("IN_APP")
                .severity("INFO")
                .title("Deployment Success")
                .message("Autonomous deployment to Staging completed successfully.")
                .isRead(false)
                .timestamp(Instant.now())
                .build();
            notificationStore.put("notif-1", notif);
        }
        return new ArrayList<>(notificationStore.values());
    }

    public NotificationModel markRead(String notificationId) {
        NotificationModel notif = notificationStore.get(notificationId);
        if (notif != null) {
            notif.setRead(true);
        }
        return notif;
    }

    public RollbackPlanModel triggerRollback(String deploymentId, String rollbackType) {
        String rollbackId = "rb-" + UUID.randomUUID();

        RollbackPlanModel plan = RollbackPlanModel.builder()
            .rollbackId(rollbackId)
            .deploymentId(deploymentId)
            .targetVersion("v1.0.0-previous")
            .rollbackType(rollbackType != null ? rollbackType : "FULL")
            .status("EXECUTED")
            .timestamp(Instant.now())
            .build();

        rollbackStore.put(rollbackId, plan);
        return plan;
    }

    public Optional<RollbackPlanModel> getRollback(String rollbackId) {
        return Optional.ofNullable(rollbackStore.get(rollbackId));
    }
}
