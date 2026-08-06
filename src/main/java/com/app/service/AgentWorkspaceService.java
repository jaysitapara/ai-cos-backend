package com.app.service;

import com.app.dto.agent.AgentLogDTO;
import com.app.dto.agent.AgentTaskDTO;
import com.app.dto.agent.ArtifactResponse;
import com.app.dto.agent.CreateWorkspaceSessionRequest;
import com.app.dto.agent.ExecutionProgressResponse;
import com.app.dto.agent.ImplementationPlanResponse;
import com.app.dto.agent.PlanApprovalRequest;
import com.app.dto.agent.UploadedFileDTO;
import com.app.dto.agent.WorkspaceSessionResponse;
import com.app.entity.AgentExecutionLogEntity;
import com.app.entity.AgentExecutionTaskEntity;
import com.app.entity.AgentWorkspaceArtifactEntity;
import com.app.entity.AgentWorkspacePlanEntity;
import com.app.entity.AgentWorkspaceSessionEntity;
import com.app.entity.UserEntity;
import com.app.exception.ResourceNotFoundException;
import com.app.repository.AgentExecutionLogRepository;
import com.app.repository.AgentExecutionTaskRepository;
import com.app.repository.AgentWorkspaceArtifactRepository;
import com.app.repository.AgentWorkspacePlanRepository;
import com.app.repository.AgentWorkspaceSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentWorkspaceService {

    private final AgentWorkspaceSessionRepository sessionRepository;
    private final AgentWorkspacePlanRepository planRepository;
    private final AgentExecutionTaskRepository taskRepository;
    private final AgentExecutionLogRepository logRepository;
    private final AgentWorkspaceArtifactRepository artifactRepository;
    private final DocumentParserService documentParserService;
    private final AgentOrchestrationEngine orchestrationEngine;
    private final ObjectMapper objectMapper;

    @Transactional
    public WorkspaceSessionResponse createSession(CreateWorkspaceSessionRequest request, UserEntity user) {
        log.info("Creating Autonomous Agent Workspace session for prompt: {}", request.getGoalPrompt());

        // Step 1: Process supporting files
        List<UploadedFileDTO> processedFiles = documentParserService.processAndExtractUploadedFiles(request.getUploadedFiles());
        String filesJson = toJson(processedFiles);

        // Step 2: Intelligent Requirement Analysis & Scope Inference
        String projectType = inferProjectType(request.getGoalPrompt());
        String complexity = inferComplexity(request.getGoalPrompt(), processedFiles);
        String estimatedScope = inferScope(complexity);

        List<String> assumptions = Arrays.asList(
                "Assume Spring Boot 3.3 REST backend with PostgreSQL database.",
                "Assume React TypeScript client with Tailwind CSS enterprise layout.",
                "Assume JWT token authentication & RBAC authorization models.",
                "Assume Docker & GitHub Actions CI/CD deployment configuration."
        );

        List<String> missingInfo = Arrays.asList(
                "Third-party payment gateway keys not provided (Using sandbox mode mock).",
                "Custom domain SSL certificate not provided (Using auto-generated TLS)."
        );

        AgentWorkspaceSessionEntity session = AgentWorkspaceSessionEntity.builder()
                .publicId(UUID.randomUUID())
                .user(user)
                .goalPrompt(request.getGoalPrompt())
                .status("AWAITING_APPROVAL")
                .projectType(projectType)
                .complexity(complexity)
                .estimatedScope(estimatedScope)
                .uploadedFilesJson(filesJson)
                .assumptionsJson(toJson(assumptions))
                .missingInfoJson(toJson(missingInfo))
                .build();
        session.setCreatedBy(user != null ? user.getEmail() : "system");
        session.setUpdatedBy(user != null ? user.getEmail() : "system");
        session = sessionRepository.save(session);

        // Step 3 & 4: Generate Implementation Plan & Architecture Specification
        AgentWorkspacePlanEntity plan = generateImplementationPlan(session, request.getGoalPrompt(), projectType);
        planRepository.save(plan);

        // Step 5: Build Execution Graph DAG
        orchestrationEngine.buildExecutionGraph(session);

        return mapToSessionResponse(session, plan);
    }

    public WorkspaceSessionResponse getSession(UUID publicId) {
        AgentWorkspaceSessionEntity session = sessionRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent Workspace Session", "publicId", publicId));
        AgentWorkspacePlanEntity plan = planRepository.findBySessionId(session.getId()).orElse(null);
        return mapToSessionResponse(session, plan);
    }

    public ImplementationPlanResponse getPlan(UUID publicId) {
        AgentWorkspaceSessionEntity session = sessionRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent Workspace Session", "publicId", publicId));
        AgentWorkspacePlanEntity plan = planRepository.findBySessionId(session.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent Workspace Plan", "sessionId", session.getId()));
        return mapToPlanResponse(plan);
    }

    @Transactional
    public WorkspaceSessionResponse approvePlan(UUID publicId, PlanApprovalRequest request) {
        AgentWorkspaceSessionEntity session = sessionRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent Workspace Session", "publicId", publicId));
        AgentWorkspacePlanEntity plan = planRepository.findBySessionId(session.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent Workspace Plan", "sessionId", session.getId()));

        if ("APPROVED".equalsIgnoreCase(request.getAction())) {
            plan.setApprovalStatus("APPROVED");
            session.setStatus("EXECUTING");
            planRepository.save(plan);
            sessionRepository.save(session);

            // Trigger Async Autonomous Multi-Agent Orchestration
            orchestrationEngine.executeOrchestrationAsync(session.getId());
        } else if ("REJECTED".equalsIgnoreCase(request.getAction())) {
            plan.setApprovalStatus("REJECTED");
            plan.setApprovalFeedback(request.getFeedback());
            session.setStatus("REJECTED");
            planRepository.save(plan);
            sessionRepository.save(session);
        } else {
            plan.setApprovalStatus("CHANGES_REQUESTED");
            plan.setApprovalFeedback(request.getFeedback());
            session.setStatus("PLANNING");
            planRepository.save(plan);
            sessionRepository.save(session);
        }

        return mapToSessionResponse(session, plan);
    }

    public ExecutionProgressResponse getProgress(UUID publicId) {
        AgentWorkspaceSessionEntity session = sessionRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent Workspace Session", "publicId", publicId));

        List<AgentExecutionTaskEntity> tasks = taskRepository.findBySessionIdOrderByExecutionOrderAsc(session.getId());
        List<AgentExecutionLogEntity> logs = logRepository.findBySessionIdOrderByTimestampAsc(session.getId());

        int total = tasks.size();
        int completed = (int) tasks.stream().filter(t -> "SUCCESS".equals(t.getStatus())).count();
        int running = (int) tasks.stream().filter(t -> "IN_PROGRESS".equals(t.getStatus())).count();
        int failed = (int) tasks.stream().filter(t -> "FAILED".equals(t.getStatus())).count();
        int waiting = total - completed - running - failed;

        int percentage = total > 0 ? (completed * 100) / total : 0;
        if ("COMPLETED".equals(session.getStatus())) percentage = 100;

        AgentExecutionTaskEntity currentTask = tasks.stream()
                .filter(t -> "IN_PROGRESS".equals(t.getStatus()))
                .findFirst()
                .orElse(null);

        String currentPhase = currentTask != null ? currentTask.getPhase() : ("COMPLETED".equals(session.getStatus()) ? "DELIVERED" : "AWAITING_APPROVAL");
        String currentRole = currentTask != null ? currentTask.getAgentRole() : "ORCHESTRATOR";
        String reasoning = currentTask != null ? currentTask.getReasoningSummary() : "System idle or execution finished.";

        List<AgentTaskDTO> taskDTOs = tasks.stream().map(this::mapToTaskDTO).collect(Collectors.toList());
        List<AgentLogDTO> logDTOs = logs.stream().map(this::mapToLogDTO).collect(Collectors.toList());

        return ExecutionProgressResponse.builder()
                .sessionStatus(session.getStatus())
                .currentPhase(currentPhase)
                .currentAgentRole(currentRole)
                .totalTasks(total)
                .completedTasks(completed)
                .runningTasks(running)
                .waitingTasks(waiting)
                .failedTasks(failed)
                .progressPercentage(percentage)
                .estimatedTimeRemaining(waiting > 0 ? (waiting * 2) + " seconds" : "Complete")
                .currentReasoningSummary(reasoning)
                .tasks(taskDTOs)
                .logs(logDTOs)
                .build();
    }

    public List<ArtifactResponse> getArtifacts(UUID publicId) {
        AgentWorkspaceSessionEntity session = sessionRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent Workspace Session", "publicId", publicId));

        return artifactRepository.findBySessionIdOrderByFilePathAsc(session.getId())
                .stream()
                .map(this::mapToArtifactResponse)
                .collect(Collectors.toList());
    }

    public List<WorkspaceSessionResponse> listUserSessions(UserEntity user) {
        List<AgentWorkspaceSessionEntity> list = (user != null)
                ? sessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                : sessionRepository.findAllByOrderByCreatedAtDesc();

        return list.stream()
                .map(s -> {
                    AgentWorkspacePlanEntity p = planRepository.findBySessionId(s.getId()).orElse(null);
                    return mapToSessionResponse(s, p);
                })
                .collect(Collectors.toList());
    }

    private AgentWorkspacePlanEntity generateImplementationPlan(AgentWorkspaceSessionEntity session, String prompt, String projectType) {
        List<String> userStories = Arrays.asList(
                "As an Administrator, I want to manage users, permissions, and system settings securely.",
                "As an End User, I want a responsive dashboard to view metrics and execute domain actions.",
                "As an Auditor, I want automated logs and real-time activity timelines for all operations."
        );

        List<String> modules = Arrays.asList(
                "Auth & Security Module (JWT, RBAC, OAuth2)",
                "Core Domain Operations & Workflow Engine",
                "Real-time Dashboard & Analytics Widgets",
                "REST API Gateway & OpenAPI Specifications",
                "DevOps & Docker Deployment Pipeline"
        );

        List<String> techStack = Arrays.asList(
                "Backend: Java 17, Spring Boot 3.3, Spring Security, JPA Hibernate",
                "Frontend: React 18, TypeScript 5, Vite, Tailwind CSS, Lucide Icons",
                "Database: PostgreSQL 16, Flyway Migrations, H2 Database",
                "DevOps: Docker, Docker Compose, GitHub Actions CI/CD"
        );

        String erDiagramMermaid =
                "erDiagram\n" +
                "    USERS ||--o{ DOMAIN_RECORDS : owns\n" +
                "    USERS ||--o{ AUDIT_LOGS : triggers\n" +
                "    USERS {\n" +
                "        bigint id PK\n" +
                "        string email UK\n" +
                "        string role\n" +
                "    }\n" +
                "    DOMAIN_RECORDS {\n" +
                "        bigint id PK\n" +
                "        bigint user_id FK\n" +
                "        string title\n" +
                "        string status\n" +
                "    }\n";

        AgentWorkspacePlanEntity plan = AgentWorkspacePlanEntity.builder()
                .publicId(UUID.randomUUID())
                .session(session)
                .executiveSummary("Target solution: " + prompt + ". Built with modular Java/Spring Boot backend and React/TypeScript frontend.")
                .businessRequirements("Provide a scalable, secure enterprise platform for " + projectType + " operations with 99.9% uptime.")
                .functionalRequirements("1. User authentication & authorization\n2. Real-time data processing\n3. Responsive dashboard & analytics\n4. Audit logging & report export")
                .nonFunctionalRequirements("Security: OWASP compliant JWT tokens\nPerformance: < 200ms API response latency\nScalability: Stateless microservices architecture")
                .userStoriesJson(toJson(userStories))
                .featuresModulesJson(toJson(modules))
                .dependenciesJson(toJson(Arrays.asList("PostgreSQL 16+", "Node.js 18+", "JDK 17+")))
                .risksJson(toJson(Arrays.asList("Third-party API rate limits", "High concurrent data writes")))
                .milestonesJson(toJson(Arrays.asList("M1: Architecture & DB DDL", "M2: Core APIs & Auth", "M3: UI Frontend Components", "M4: QA & Deployment")))
                .systemArchitecture("Layered Clean Architecture: Controller -> Service -> Repository -> Database with Async Queue Event Bus.")
                .folderStructure("backend/\n  src/main/java/com/app/\n    controller/\n    service/\n    entity/\nfrontend/\n  src/\n    components/\n    pages/\n    services/")
                .databaseDesign("Relational schema with normalized tables, Flyway migrations, and indexed foreign keys.")
                .apiDesign("RESTful JSON API with Standardized ProblemDetail error formats and Swagger OpenAPI UI.")
                .erDiagramMermaid(erDiagramMermaid)
                .techStackJson(toJson(techStack))
                .authFlow("Stateless JWT Bearer Token flow with Refresh Token rotation.")
                .deploymentArchitecture("Multi-stage Docker containerization with docker-compose orchestration.")
                .approvalStatus("PENDING")
                .build();

        plan.setCreatedBy(session.getCreatedBy());
        plan.setUpdatedBy(session.getUpdatedBy());
        return plan;
    }

    private String inferProjectType(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("hospital") || lower.contains("medical") || lower.contains("health")) return "Healthcare & Hospital System";
        if (lower.contains("ecommerce") || lower.contains("shop") || lower.contains("store")) return "E-Commerce SaaS Platform";
        if (lower.contains("bank") || lower.contains("fintech") || lower.contains("payment")) return "Fintech & Financial Gateway";
        return "Enterprise Web Application System";
    }

    private String inferComplexity(String prompt, List<UploadedFileDTO> files) {
        int length = prompt.length();
        if (files != null && !files.isEmpty()) return "HIGH";
        if (length > 100) return "MEDIUM";
        return "STANDARD";
    }

    private String inferScope(String complexity) {
        if ("HIGH".equals(complexity)) return "16 Sub-Agent Orchestration Tasks (Full Scale)";
        return "12 Sub-Agent Orchestration Tasks (Standard)";
    }

    private WorkspaceSessionResponse mapToSessionResponse(AgentWorkspaceSessionEntity session, AgentWorkspacePlanEntity plan) {
        return WorkspaceSessionResponse.builder()
                .publicId(session.getPublicId())
                .goalPrompt(session.getGoalPrompt())
                .status(session.getStatus())
                .projectType(session.getProjectType())
                .complexity(session.getComplexity())
                .estimatedScope(session.getEstimatedScope())
                .uploadedFiles(fromJson(session.getUploadedFilesJson(), new TypeReference<List<UploadedFileDTO>>() {}))
                .assumptions(fromJson(session.getAssumptionsJson(), new TypeReference<List<String>>() {}))
                .missingInfo(fromJson(session.getMissingInfoJson(), new TypeReference<List<String>>() {}))
                .plan(plan != null ? mapToPlanResponse(plan) : null)
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private ImplementationPlanResponse mapToPlanResponse(AgentWorkspacePlanEntity plan) {
        return ImplementationPlanResponse.builder()
                .publicId(plan.getPublicId())
                .executiveSummary(plan.getExecutiveSummary())
                .businessRequirements(plan.getBusinessRequirements())
                .functionalRequirements(plan.getFunctionalRequirements())
                .nonFunctionalRequirements(plan.getNonFunctionalRequirements())
                .userStories(fromJson(plan.getUserStoriesJson(), new TypeReference<List<String>>() {}))
                .featuresModules(fromJson(plan.getFeaturesModulesJson(), new TypeReference<List<String>>() {}))
                .dependencies(fromJson(plan.getDependenciesJson(), new TypeReference<List<String>>() {}))
                .risks(fromJson(plan.getRisksJson(), new TypeReference<List<String>>() {}))
                .milestones(fromJson(plan.getMilestonesJson(), new TypeReference<List<String>>() {}))
                .systemArchitecture(plan.getSystemArchitecture())
                .folderStructure(plan.getFolderStructure())
                .databaseDesign(plan.getDatabaseDesign())
                .apiDesign(plan.getApiDesign())
                .erDiagramMermaid(plan.getErDiagramMermaid())
                .techStack(fromJson(plan.getTechStackJson(), new TypeReference<List<String>>() {}))
                .authFlow(plan.getAuthFlow())
                .deploymentArchitecture(plan.getDeploymentArchitecture())
                .approvalStatus(plan.getApprovalStatus())
                .approvalFeedback(plan.getApprovalFeedback())
                .build();
    }

    private AgentTaskDTO mapToTaskDTO(AgentExecutionTaskEntity t) {
        List<String> deps = fromJson(t.getDependenciesJson(), new TypeReference<List<String>>() {});
        return AgentTaskDTO.builder()
                .id(t.getId())
                .taskKey(t.getTaskKey())
                .title(t.getTitle())
                .agentRole(t.getAgentRole())
                .phase(t.getPhase())
                .executionOrder(t.getExecutionOrder())
                .dependencies(deps != null ? deps : new ArrayList<>())
                .status(t.getStatus())
                .retries(t.getRetries())
                .reasoningSummary(t.getReasoningSummary())
                .outputSummary(t.getOutputSummary())
                .startedAt(t.getStartedAt())
                .completedAt(t.getCompletedAt())
                .build();
    }

    private AgentLogDTO mapToLogDTO(AgentExecutionLogEntity l) {
        return AgentLogDTO.builder()
                .id(l.getId())
                .taskId(l.getTask() != null ? l.getTask().getId() : null)
                .agentRole(l.getAgentRole())
                .logLevel(l.getLogLevel())
                .message(l.getMessage())
                .reasoning(l.getReasoning())
                .timestamp(l.getTimestamp())
                .build();
    }

    private ArtifactResponse mapToArtifactResponse(AgentWorkspaceArtifactEntity a) {
        return ArtifactResponse.builder()
                .publicId(a.getPublicId())
                .filePath(a.getFilePath())
                .fileName(a.getFileName())
                .artifactType(a.getArtifactType())
                .content(a.getContent())
                .agentRole(a.getAgentRole())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            return null;
        }
    }
}
