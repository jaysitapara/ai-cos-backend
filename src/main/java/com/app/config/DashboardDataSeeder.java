package com.app.config;

import com.app.entity.ActivityEntity;
import com.app.entity.ChatEntity;
import com.app.entity.DocumentEntity;
import com.app.entity.FileEntity;
import com.app.entity.MeetingEntity;
import com.app.entity.NotificationEntity;
import com.app.entity.OrganizationEntity;
import com.app.entity.ProjectEntity;
import com.app.entity.TaskEntity;
import com.app.entity.UserEntity;
import com.app.entity.WorkspaceEntity;
import com.app.enums.UserRole;
import com.app.enums.UserStatus;
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
import com.app.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DashboardDataSeeder implements CommandLineRunner {

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
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (organizationRepository.count() > 0) {
            log.info("Dashboard data already seeded. Skipping initialization.");
            return;
        }

        log.info("Seeding enterprise Dashboard demo data...");

        // Seed Default Admin User if no users exist
        UserEntity defaultUser = userRepository.findAll().stream().findFirst().orElseGet(() -> {
            UserEntity user = UserEntity.builder()
                .publicId(UUID.randomUUID())
                .email("admin@aicos.io")
                .fullName("Enterprise Architect")
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .role(UserRole.ROLE_ADMIN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .build();
            return userRepository.save(user);
        });

        // 1. Organizations & Workspaces
        OrganizationEntity org1 = organizationRepository.save(OrganizationEntity.builder()
            .publicId(UUID.randomUUID())
            .name("Acme Enterprise AI")
            .slug("acme-ai")
            .planType("ENTERPRISE_PLUS")
            .build());

        OrganizationEntity org2 = organizationRepository.save(OrganizationEntity.builder()
            .publicId(UUID.randomUUID())
            .name("Cyberdyne Defense Labs")
            .slug("cyberdyne")
            .planType("ENTERPRISE")
            .build());

        WorkspaceEntity ws1 = workspaceRepository.save(WorkspaceEntity.builder()
            .publicId(UUID.randomUUID())
            .organization(org1)
            .name("Production Workspace")
            .slug("production")
            .build());

        WorkspaceEntity ws2 = workspaceRepository.save(WorkspaceEntity.builder()
            .publicId(UUID.randomUUID())
            .organization(org1)
            .name("R&D Swarm Workspace")
            .slug("rd-swarm")
            .build());

        // 2. Projects
        ProjectEntity proj1 = projectRepository.save(ProjectEntity.builder()
            .publicId(UUID.randomUUID())
            .workspace(ws1)
            .name("Autonomous AI Agent Engine")
            .description("Multi-agent orchestration platform for enterprise workflow automation")
            .status("IN_PROGRESS")
            .progress(78)
            .dueDate(DateTimeUtil.nowUtc().plusDays(14))
            .build());

        ProjectEntity proj2 = projectRepository.save(ProjectEntity.builder()
            .publicId(UUID.randomUUID())
            .workspace(ws1)
            .name("Knowledge Graph RAG Pipeline")
            .description("Vector database indexing and graph retrieval augmentation engine")
            .status("IN_PROGRESS")
            .progress(62)
            .dueDate(DateTimeUtil.nowUtc().plusDays(30))
            .build());

        ProjectEntity proj3 = projectRepository.save(ProjectEntity.builder()
            .publicId(UUID.randomUUID())
            .workspace(ws2)
            .name("Zero-Trust IAM & Security Audit")
            .description("Role-based access control and immutable compliance audit logging")
            .status("COMPLETED")
            .progress(100)
            .dueDate(DateTimeUtil.nowUtc().minusDays(5))
            .build());

        // 3. Tasks
        taskRepository.saveAll(List.of(
            TaskEntity.builder().publicId(UUID.randomUUID()).project(proj1).assignee(defaultUser).title("Optimize vector embeddings pipeline throughput").status("IN_PROGRESS").priority("URGENT").dueDate(DateTimeUtil.nowUtc().plusDays(2)).build(),
            TaskEntity.builder().publicId(UUID.randomUUID()).project(proj1).assignee(defaultUser).title("Configure Argon2id password hash iterations").status("COMPLETED").priority("HIGH").dueDate(DateTimeUtil.nowUtc().minusDays(1)).build(),
            TaskEntity.builder().publicId(UUID.randomUUID()).project(proj2).assignee(defaultUser).title("Deploy PostgreSQL Flyway V4 schema migration").status("IN_PROGRESS").priority("HIGH").dueDate(DateTimeUtil.nowUtc().plusDays(3)).build(),
            TaskEntity.builder().publicId(UUID.randomUUID()).project(proj2).assignee(defaultUser).title("Implement Redis cache for global search command palette").status("TODO").priority("MEDIUM").dueDate(DateTimeUtil.nowUtc().plusDays(5)).build(),
            TaskEntity.builder().publicId(UUID.randomUUID()).project(proj3).assignee(defaultUser).title("Verify OAuth2 Google & GitHub callback handlers").status("COMPLETED").priority("URGENT").dueDate(DateTimeUtil.nowUtc().minusDays(2)).build()
        ));

        // 4. AI Chats
        chatRepository.saveAll(List.of(
            ChatEntity.builder().publicId(UUID.randomUUID()).user(defaultUser).title("Architecture Design: Multi-Agent Topology").lastMessage("The orchestrator agent delegates tasks using pub/sub queues.").unreadCount(2).build(),
            ChatEntity.builder().publicId(UUID.randomUUID()).user(defaultUser).title("Database Query Optimization Advisor").lastMessage("INDEX idx_tasks_status increased read throughput by 42%.").unreadCount(0).build(),
            ChatEntity.builder().publicId(UUID.randomUUID()).user(defaultUser).title("Security Compliance Audit Checklist").lastMessage("OWASP Top 10 automated scan passed with zero critical findings.").unreadCount(1).build()
        ));

        // 5. Meetings
        OffsetDateTime now = DateTimeUtil.nowUtc();
        meetingRepository.saveAll(List.of(
            MeetingEntity.builder().publicId(UUID.randomUUID()).user(defaultUser).title("AI COS Architecture Sync").startTime(now.plusHours(2)).endTime(now.plusHours(3)).meetingLink("https://meet.aicos.io/arch-sync").attendeesCount(6).build(),
            MeetingEntity.builder().publicId(UUID.randomUUID()).user(defaultUser).title("Sprint 14 Product Roadmap Review").startTime(now.plusDays(1).plusHours(4)).endTime(now.plusDays(1).plusHours(5)).meetingLink("https://meet.aicos.io/sprint-14").attendeesCount(12).build(),
            MeetingEntity.builder().publicId(UUID.randomUUID()).user(defaultUser).title("Enterprise Security & Compliance Review").startTime(now.plusDays(3)).endTime(now.plusDays(3).plusHours(1)).meetingLink("https://meet.aicos.io/security").attendeesCount(4).build()
        ));

        // 6. Documents & Files
        documentRepository.saveAll(List.of(
            DocumentEntity.builder().publicId(UUID.randomUUID()).workspace(ws1).title("AI-COS System Architecture Specification v2.4").docType("SPECIFICATION").sizeBytes(4_200_000L).build(),
            DocumentEntity.builder().publicId(UUID.randomUUID()).workspace(ws1).title("Zero-Trust Security & Identity SRS").docType("SRS").sizeBytes(1_850_000L).build(),
            DocumentEntity.builder().publicId(UUID.randomUUID()).workspace(ws2).title("Autonomous Agent Swarm Playbook").docType("MANUAL").sizeBytes(6_100_000L).build()
        ));

        fileRepository.saveAll(List.of(
            FileEntity.builder().publicId(UUID.randomUUID()).workspace(ws1).name("architecture_diagram_v3").extension("png").sizeBytes(12_400_000L).build(),
            FileEntity.builder().publicId(UUID.randomUUID()).workspace(ws1).name("flyway_db_schema_export").extension("sql").sizeBytes(850_000L).build(),
            FileEntity.builder().publicId(UUID.randomUUID()).workspace(ws2).name("agent_cluster_metrics").extension("json").sizeBytes(2_300_000L).build()
        ));

        // 7. Notifications
        notificationRepository.saveAll(List.of(
            NotificationEntity.builder().publicId(UUID.randomUUID()).user(defaultUser).title("High Priority Task Assigned").message("You were assigned 'Optimize vector embeddings pipeline' in Project AI Agent Engine").type("TASK").isRead(false).build(),
            NotificationEntity.builder().publicId(UUID.randomUUID()).user(defaultUser).title("Security Audit Completed").message("Argon2id password hashing migration V3 succeeded with 100% compliance.").type("SECURITY").isRead(false).build(),
            NotificationEntity.builder().publicId(UUID.randomUUID()).user(defaultUser).title("New Team Member Joined").message("Sarah Jenkins joined Acme Enterprise AI organization.").type("SYSTEM").isRead(true).build()
        ));

        // 8. Activities
        activityRepository.saveAll(List.of(
            ActivityEntity.builder().publicId(UUID.randomUUID()).user(defaultUser).action("DEPLOYED").description("Deployed Spring Boot 3 backend build v2.1.0 to staging cluster").entityType("DEPLOYMENT").build(),
            ActivityEntity.builder().publicId(UUID.randomUUID()).user(defaultUser).action("UPDATED").description("Updated project status to IN_PROGRESS for Autonomous AI Agent Engine").entityType("PROJECT").build(),
            ActivityEntity.builder().publicId(UUID.randomUUID()).user(defaultUser).action("COMPLETED").description("Completed task 'Configure Argon2id password hash iterations'").entityType("TASK").build()
        ));

        log.info("Enterprise Dashboard demo data successfully seeded!");
    }
}
