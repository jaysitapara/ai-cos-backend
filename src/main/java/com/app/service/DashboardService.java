package com.app.service;

import com.app.dto.response.ActivityResponse;
import com.app.dto.response.ChatResponse;
import com.app.dto.response.DashboardSummaryResponse;
import com.app.dto.response.DocumentResponse;
import com.app.dto.response.FileResponse;
import com.app.dto.response.MeetingResponse;
import com.app.dto.response.NotificationResponse;
import com.app.dto.response.OrganizationResponse;
import com.app.dto.response.ProjectResponse;
import com.app.dto.response.SearchResultResponse;
import com.app.dto.response.TaskResponse;

import java.util.List;
import java.util.UUID;

public interface DashboardService {

    DashboardSummaryResponse getSummary(UUID userPublicId);

    List<ProjectResponse> getRecentProjects(UUID userPublicId);

    List<TaskResponse> getMyTasks(UUID userPublicId);

    List<ChatResponse> getRecentChats(UUID userPublicId);

    List<MeetingResponse> getUpcomingMeetings(UUID userPublicId);

    List<DocumentResponse> getRecentDocuments(UUID userPublicId);

    List<FileResponse> getRecentFiles(UUID userPublicId);

    List<NotificationResponse> getNotifications(UUID userPublicId);

    List<ActivityResponse> getActivities(UUID userPublicId);

    List<OrganizationResponse> getOrganizations(UUID userPublicId);

    SearchResultResponse search(UUID userPublicId, String query);

    void markTaskStatus(UUID userPublicId, UUID taskPublicId, String status);

    void markNotificationAsRead(UUID userPublicId, UUID notificationPublicId);
}
