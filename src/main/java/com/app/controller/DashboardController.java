package com.app.controller;

import com.app.common.ApiConstants;
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
import com.app.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard Module", description = "Enterprise Dashboard Command Center APIs for Summary, Widgets, and Global Search")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get high-level dashboard metrics and summary counters")
    public ResponseEntity<DashboardSummaryResponse> getSummary(Authentication authentication) {
        UUID userPublicId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(dashboardService.getSummary(userPublicId));
    }

    @GetMapping("/projects")
    @Operation(summary = "Get active projects for dashboard widget")
    public ResponseEntity<List<ProjectResponse>> getProjects(Authentication authentication) {
        UUID userPublicId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(dashboardService.getRecentProjects(userPublicId));
    }

    @GetMapping("/tasks")
    @Operation(summary = "Get assigned tasks for dashboard widget")
    public ResponseEntity<List<TaskResponse>> getTasks(Authentication authentication) {
        UUID userPublicId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(dashboardService.getMyTasks(userPublicId));
    }

    @GetMapping("/chats")
    @Operation(summary = "Get recent AI chat conversations for dashboard widget")
    public ResponseEntity<List<ChatResponse>> getChats(Authentication authentication) {
        UUID userPublicId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(dashboardService.getRecentChats(userPublicId));
    }

    @GetMapping("/meetings")
    @Operation(summary = "Get upcoming meetings for dashboard widget")
    public ResponseEntity<List<MeetingResponse>> getMeetings(Authentication authentication) {
        UUID userPublicId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(dashboardService.getUpcomingMeetings(userPublicId));
    }

    @GetMapping("/documents")
    @Operation(summary = "Get recent knowledge documents for dashboard widget")
    public ResponseEntity<List<DocumentResponse>> getDocuments(Authentication authentication) {
        UUID userPublicId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(dashboardService.getRecentDocuments(userPublicId));
    }

    @GetMapping("/files")
    @Operation(summary = "Get recent workspace files for dashboard widget")
    public ResponseEntity<List<FileResponse>> getFiles(Authentication authentication) {
        UUID userPublicId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(dashboardService.getRecentFiles(userPublicId));
    }

    @GetMapping("/notifications")
    @Operation(summary = "Get notifications for header and dashboard widget")
    public ResponseEntity<List<NotificationResponse>> getNotifications(Authentication authentication) {
        UUID userPublicId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(dashboardService.getNotifications(userPublicId));
    }

    @GetMapping("/activities")
    @Operation(summary = "Get system & team activity timeline for dashboard widget")
    public ResponseEntity<List<ActivityResponse>> getActivities(Authentication authentication) {
        UUID userPublicId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(dashboardService.getActivities(userPublicId));
    }

    @GetMapping("/organizations")
    @Operation(summary = "Get user organizations and workspaces for header switcher")
    public ResponseEntity<List<OrganizationResponse>> getOrganizations(Authentication authentication) {
        UUID userPublicId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(dashboardService.getOrganizations(userPublicId));
    }

    @GetMapping("/search")
    @Operation(summary = "Global command palette search endpoint")
    public ResponseEntity<SearchResultResponse> search(@RequestParam(value = "q", required = false, defaultValue = "") String query, Authentication authentication) {
        UUID userPublicId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(dashboardService.search(userPublicId, query));
    }

    @PatchMapping("/tasks/{public_id}/status")
    @Operation(summary = "Update task completion status from dashboard task widget")
    public ResponseEntity<Void> updateTaskStatus(@PathVariable("public_id") UUID taskPublicId, @RequestParam("status") String status, Authentication authentication) {
        UUID userPublicId = UUID.fromString(authentication.getName());
        dashboardService.markTaskStatus(userPublicId, taskPublicId, status);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/notifications/{public_id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Void> markNotificationAsRead(@PathVariable("public_id") UUID notificationPublicId, Authentication authentication) {
        UUID userPublicId = UUID.fromString(authentication.getName());
        dashboardService.markNotificationAsRead(userPublicId, notificationPublicId);
        return ResponseEntity.ok().build();
    }
}
