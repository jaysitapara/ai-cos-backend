package com.app.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DashboardSummaryResponse(
    long totalProjects,
    long activeTasks,
    long completedTasks,
    long upcomingMeetings,
    long unreadNotifications,
    long activeChats,
    long storageUsedBytes,
    long totalStorageQuotaBytes,
    int productivityScore,
    String systemStatus,
    String currentOrganizationName,
    String currentWorkspaceName
) {}
