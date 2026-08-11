package com.app.model.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationModel {
    private String notificationId;
    private String type; // IN_APP, EMAIL, WEBHOOK
    private String severity; // INFO, WARNING, CRITICAL
    private String title;
    private String message;
    private boolean isRead;
    private Instant timestamp;
}
