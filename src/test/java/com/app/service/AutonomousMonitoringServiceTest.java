package com.app.service;

import com.app.model.monitoring.NotificationModel;
import com.app.model.monitoring.RollbackPlanModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AutonomousMonitoringServiceTest {

    private AutonomousMonitoringService service;

    @BeforeEach
    void setUp() {
        service = new AutonomousMonitoringService();
    }

    @Test
    void testNotificationsAndRollback() {
        List<NotificationModel> notifications = service.getNotifications();
        assertFalse(notifications.isEmpty());

        NotificationModel readNotif = service.markRead(notifications.get(0).getNotificationId());
        assertTrue(readNotif.isRead());

        RollbackPlanModel rollback = service.triggerRollback("dep-99", "FULL");
        assertNotNull(rollback);
        assertEquals("dep-99", rollback.getDeploymentId());
        assertEquals("EXECUTED", rollback.getStatus());
    }
}
