package com.app.service.impl;

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
import com.app.entity.NotificationEntity;
import com.app.entity.TaskEntity;
import com.app.entity.UserEntity;
import com.app.exception.ResourceNotFoundException;
import com.app.repository.ActivityRepository;
import com.app.repository.ChatRepository;
import com.app.repository.DocumentRepository;
import com.app.repository.FileRepository;
import com.app.repository.MeetingRepository;
import com.app.repository.NotificationRepository;
import com.app.repository.OrganizationRepository;
import com.app.repository.ProjectRepository;
import com.app.repository.TaskRepository;
import com.app.repository.UserRepository;
import com.app.repository.WorkspaceRepository;
import com.app.service.DashboardService;
import com.app.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final long DEFAULT_STORAGE_QUOTA_BYTES = 50L * 1024 * 1024 * 1024; // 50 GB

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final ChatRepository chatRepository;
    private final MeetingRepository meetingRepository;
    private final DocumentRepository documentRepository;
    private final FileRepository fileRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityRepository activityRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(UUID userPublicId) {
        UserEntity user = findUser(userPublicId);

        long totalProjects = projectRepository.countByDeletedAtIsNull();
        long activeTasks = taskRepository.countByStatusAndDeletedAtIsNull("IN_PROGRESS") + taskRepository.countByStatusAndDeletedAtIsNull("TODO");
        long completedTasks = taskRepository.countByStatusAndDeletedAtIsNull("COMPLETED");
        long upcomingMeetings = meetingRepository.countByDeletedAtIsNull();
        long unreadNotifications = notificationRepository.countByUserAndIsReadFalseAndDeletedAtIsNull(user);
        long activeChats = chatRepository.countByDeletedAtIsNull();
        long storageUsed = fileRepository.calculateTotalStorageUsed();

        int productivityScore = calculateProductivityScore(completedTasks, activeTasks);

        return new DashboardSummaryResponse(
            totalProjects,
            activeTasks,
            completedTasks,
            upcomingMeetings,
            unreadNotifications,
            activeChats,
            storageUsed,
            DEFAULT_STORAGE_QUOTA_BYTES,
            productivityScore,
            "OPERATIONAL",
            "Acme Enterprise AI",
            "Production Workspace"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getRecentProjects(UUID userPublicId) {
        return projectRepository.findAllByDeletedAtIsNullOrderByUpdatedAtDesc(PageRequest.of(0, 6))
            .stream()
            .map(p -> new ProjectResponse(
                p.getPublicId(),
                p.getName(),
                p.getDescription(),
                p.getStatus(),
                p.getProgress(),
                p.getDueDate(),
                p.getUpdatedAt()
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(UUID userPublicId) {
        UserEntity user = findUser(userPublicId);
        return taskRepository.findAllByAssigneeAndDeletedAtIsNullOrderByDueDateAsc(user, PageRequest.of(0, 10))
            .stream()
            .map(t -> new TaskResponse(
                t.getPublicId(),
                t.getTitle(),
                t.getStatus(),
                t.getPriority(),
                t.getProject() != null ? t.getProject().getName() : "General",
                t.getAssignee() != null ? t.getAssignee().getFullName() : "Unassigned",
                t.getDueDate()
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatResponse> getRecentChats(UUID userPublicId) {
        UserEntity user = findUser(userPublicId);
        return chatRepository.findAllByUserAndDeletedAtIsNullOrderByUpdatedAtDesc(user, PageRequest.of(0, 6))
            .stream()
            .map(c -> new ChatResponse(
                c.getPublicId(),
                c.getTitle(),
                c.getLastMessage(),
                c.getUnreadCount(),
                c.getUpdatedAt()
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingResponse> getUpcomingMeetings(UUID userPublicId) {
        return meetingRepository.findAllByStartTimeAfterAndDeletedAtIsNullOrderByStartTimeAsc(DateTimeUtil.nowUtc().minusHours(1), PageRequest.of(0, 5))
            .stream()
            .map(m -> new MeetingResponse(
                m.getPublicId(),
                m.getTitle(),
                m.getStartTime(),
                m.getEndTime(),
                m.getMeetingLink(),
                m.getAttendeesCount()
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getRecentDocuments(UUID userPublicId) {
        return documentRepository.findAllByDeletedAtIsNullOrderByUpdatedAtDesc(PageRequest.of(0, 6))
            .stream()
            .map(d -> new DocumentResponse(
                d.getPublicId(),
                d.getTitle(),
                d.getDocType(),
                d.getSizeBytes(),
                d.getUpdatedAt()
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileResponse> getRecentFiles(UUID userPublicId) {
        return fileRepository.findAllByDeletedAtIsNullOrderByUpdatedAtDesc(PageRequest.of(0, 6))
            .stream()
            .map(f -> new FileResponse(
                f.getPublicId(),
                f.getName(),
                f.getExtension(),
                f.getSizeBytes(),
                f.getUpdatedAt()
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(UUID userPublicId) {
        UserEntity user = findUser(userPublicId);
        return notificationRepository.findAllByUserAndDeletedAtIsNullOrderByCreatedAtDesc(user, PageRequest.of(0, 10))
            .stream()
            .map(n -> new NotificationResponse(
                n.getPublicId(),
                n.getTitle(),
                n.getMessage(),
                n.getType(),
                n.isRead(),
                n.getCreatedAt()
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponse> getActivities(UUID userPublicId) {
        return activityRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc(PageRequest.of(0, 10))
            .stream()
            .map(a -> new ActivityResponse(
                a.getPublicId(),
                a.getUser() != null ? a.getUser().getFullName() : "System",
                a.getAction(),
                a.getDescription(),
                a.getEntityType(),
                a.getCreatedAt()
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationResponse> getOrganizations(UUID userPublicId) {
        return organizationRepository.findAllByDeletedAtIsNullOrderByNameAsc()
            .stream()
            .map(org -> {
                List<OrganizationResponse.WorkspaceResponse> workspaces = workspaceRepository
                    .findAllByOrganizationAndDeletedAtIsNullOrderByNameAsc(org)
                    .stream()
                    .map(ws -> new OrganizationResponse.WorkspaceResponse(ws.getPublicId(), ws.getName(), ws.getSlug()))
                    .toList();
                return new OrganizationResponse(
                    org.getPublicId(),
                    org.getName(),
                    org.getSlug(),
                    org.getPlanType(),
                    workspaces
                );
            })
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResultResponse search(UUID userPublicId, String query) {
        String q = query == null ? "" : query.toLowerCase().trim();
        List<SearchResultResponse.SearchItem> items = new ArrayList<>();

        projectRepository.findAllByDeletedAtIsNull().stream()
            .filter(p -> p.getName().toLowerCase().contains(q) || (p.getDescription() != null && p.getDescription().toLowerCase().contains(q)))
            .limit(3)
            .forEach(p -> items.add(new SearchResultResponse.SearchItem(
                p.getPublicId().toString(),
                p.getName(),
                "Project • " + p.getStatus(),
                "PROJECT",
                "/projects/" + p.getPublicId()
            )));

        taskRepository.findAllByDeletedAtIsNull().stream()
            .filter(t -> t.getTitle().toLowerCase().contains(q))
            .limit(3)
            .forEach(t -> items.add(new SearchResultResponse.SearchItem(
                t.getPublicId().toString(),
                t.getTitle(),
                "Task • " + t.getPriority() + " Priority",
                "TASK",
                "/tasks/" + t.getPublicId()
            )));

        return new SearchResultResponse(items);
    }

    @Override
    @Transactional
    public void markTaskStatus(UUID userPublicId, UUID taskPublicId, String status) {
        TaskEntity task = taskRepository.findByPublicIdAndDeletedAtIsNull(taskPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        task.setStatus(status);
        taskRepository.save(task);
    }

    @Override
    @Transactional
    public void markNotificationAsRead(UUID userPublicId, UUID notificationPublicId) {
        NotificationEntity notification = notificationRepository.findByPublicIdAndDeletedAtIsNull(notificationPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    private UserEntity findUser(UUID publicId) {
        return userRepository.findByPublicIdAndDeletedAtIsNull(publicId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private int calculateProductivityScore(long completedTasks, long activeTasks) {
        long total = completedTasks + activeTasks;
        if (total == 0) return 92;
        return (int) Math.min(100, Math.max(50, (completedTasks * 100) / total + 20));
    }
}
