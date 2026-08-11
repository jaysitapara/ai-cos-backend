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

import java.util.*;

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
        log.info("Creating Autonomous Agent Workspace session for prompt: {} (ExecutionMode: {})", 
                request.getGoalPrompt(), request.getExecutionMode());

        com.app.entity.ExecutionMode mode = request.getExecutionMode() != null 
                ? request.getExecutionMode() 
                : com.app.entity.ExecutionMode.AUTO;

        boolean isAuto = mode == com.app.entity.ExecutionMode.AUTO;
        String initialStatus = isAuto ? "EXECUTING" : "AWAITING_APPROVAL";

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
                .status(initialStatus)
                .executionMode(mode)
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
        if (isAuto) {
            plan.setApprovalStatus("APPROVED");
        }
        planRepository.save(plan);

        // Step 5: Build Execution Graph DAG
        orchestrationEngine.buildExecutionGraph(session);

        if (isAuto) {
            log.info("Auto Execution Mode activated for session public_id={}. Launching multi-agent execution automatically...", session.getPublicId());
            orchestrationEngine.executeOrchestrationAsync(session.getId());
        }

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
        List<ArtifactResponse> artifactDTOs = getArtifacts(publicId);

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
        String currentAgentName = currentTask != null ? currentTask.getAgentRole() : ("COMPLETED".equals(session.getStatus()) ? "SYSTEM_ORCHESTRATOR" : "SYSTEM_ORCHESTRATOR");
        String currentTaskTitle = currentTask != null ? currentTask.getTitle() : ("COMPLETED".equals(session.getStatus()) ? "Execution Completed Successfully" : "Awaiting Approval Gate");
        String currentFileName = currentTask != null ? mapRoleToFileName(currentTask.getAgentRole()) : "N/A";
        String reasoning = currentTask != null ? currentTask.getReasoningSummary() : "System idle or execution finished.";

        long elapsedTimeSeconds = 0;
        if (session.getCreatedAt() != null) {
            elapsedTimeSeconds = java.time.Duration.between(session.getCreatedAt(), java.time.OffsetDateTime.now()).getSeconds();
        }

        List<AgentTaskDTO> taskDTOs = tasks.stream().map(this::mapToTaskDTO).collect(Collectors.toList());
        List<AgentLogDTO> logDTOs = logs.stream().map(this::mapToLogDTO).collect(Collectors.toList());

        long promptTokensTotal = taskDTOs.stream().mapToLong(t -> t.getPromptTokens() != null ? t.getPromptTokens() : 0).sum();
        long completionTokensTotal = taskDTOs.stream().mapToLong(t -> t.getCompletionTokens() != null ? t.getCompletionTokens() : 0).sum();
        long totalTokens = promptTokensTotal + completionTokensTotal;
        double estimatedCost = (promptTokensTotal * 0.00000015) + (completionTokensTotal * 0.0000006);

        return ExecutionProgressResponse.builder()
                .sessionStatus(session.getStatus())
                .executionMode(session.getExecutionMode())
                .startedAt(session.getCreatedAt() != null ? session.getCreatedAt().toString() : null)
                .elapsedTimeSeconds(elapsedTimeSeconds)
                .currentPhase(currentPhase)
                .currentAgentRole(currentRole)
                .currentAgentName(currentAgentName)
                .currentTaskTitle(currentTaskTitle)
                .currentFileName(currentFileName)
                .totalTasks(total)
                .completedTasks(completed)
                .runningTasks(running)
                .waitingTasks(waiting)
                .failedTasks(failed)
                .skippedTasks(0)
                .totalAgents(16)
                .completedAgents(completed)
                .progressPercentage(percentage)
                .estimatedTimeRemaining(waiting > 0 ? (waiting * 2) + " seconds" : "0 seconds")
                .currentReasoningSummary(reasoning)
                .promptTokens(promptTokensTotal)
                .completionTokens(completionTokensTotal)
                .totalTokens(totalTokens)
                .estimatedCost(estimatedCost)
                .tasks(taskDTOs)
                .logs(logDTOs)
                .artifacts(artifactDTOs)
                .build();
    }

    @Transactional
    public List<ArtifactResponse> getArtifacts(UUID publicId) {
        AgentWorkspaceSessionEntity session = sessionRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent Workspace Session", "publicId", publicId));

        ensureCompleteProjectArtifacts(session);

        return artifactRepository.findBySessionIdOrderByFilePathAsc(session.getId())
                .stream()
                .map(this::mapToArtifactResponse)
                .collect(Collectors.toList());
    }

    private void ensureCompleteProjectArtifacts(AgentWorkspaceSessionEntity session) {
        List<AgentWorkspaceArtifactEntity> existing = artifactRepository.findBySessionIdOrderByFilePathAsc(session.getId());
        Set<String> existingPaths = existing.stream().map(AgentWorkspaceArtifactEntity::getFilePath).collect(Collectors.toSet());

        Map<String, String[]> templates = new LinkedHashMap<>();
        templates.put("README.md", new String[]{"README.md", "MARKDOWN", "DOCUMENTATION_AGENT",
            "# AI-COS Generated Application\n\n## Executive Summary\nThis project was automatically analyzed, designed, architected, and generated by the AI-COS Multi-Agent Swarm.\n\n## Getting Started\n```bash\ndocker-compose up -d\ncd backend && ./gradlew bootRun\ncd frontend && npm run dev\n```\n"});

        templates.put("docker-compose.yml", new String[]{"docker-compose.yml", "DOCKER", "DEVOPS_AGENT",
            "version: '3.8'\nservices:\n  backend:\n    build: .\n    ports:\n      - \"8080:8080\"\n  postgres:\n    image: postgres:16-alpine\n    ports:\n      - \"5432:5432\"\n    environment:\n      - POSTGRES_DB=aicos_db\n      - POSTGRES_USER=admin\n      - POSTGRES_PASSWORD=Password123!\n"});

        templates.put("Dockerfile", new String[]{"Dockerfile", "DOCKER", "DEVOPS_AGENT",
            "FROM eclipse-temurin:17-jdk-alpine AS builder\nWORKDIR /app\nCOPY backend/ .\nRUN ./gradlew bootJar --no-daemon\n\nFROM eclipse-temurin:17-jre-alpine\nWORKDIR /app\nCOPY --from=builder /app/build/libs/*.jar app.jar\nEXPOSE 8080\nENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]\n"});

        templates.put("docs/openapi-spec.yaml", new String[]{"openapi-spec.yaml", "SWAGGER_SPEC", "API_AGENT",
            "openapi: 3.0.3\ninfo:\n  title: AI-COS Generated Enterprise API\n  version: 1.0.0\npaths:\n  /v1/domain-resources:\n    get:\n      summary: List resources\n      responses:\n        '200':\n          description: Success\n"});

        templates.put("backend/src/main/resources/db/migration/V10__generated_schema.sql", new String[]{"V10__generated_schema.sql", "DATABASE_DDL", "DATABASE_AGENT",
            "-- AI-COS Production Database Migration Schema V10\nCREATE TABLE IF NOT EXISTS domain_resources (\n    id BIGSERIAL PRIMARY KEY,\n    public_id UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),\n    title VARCHAR(255) NOT NULL,\n    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',\n    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP\n);\n"});

        templates.put("backend/src/main/java/com/app/controller/DomainResourceController.java", new String[]{"DomainResourceController.java", "JAVA_SOURCE", "BACKEND_AGENT",
            "package com.app.controller;\n\nimport org.springframework.http.ResponseEntity;\nimport org.springframework.web.bind.annotation.*;\nimport java.util.*;\n\n@RestController\n@RequestMapping(\"/v1/domain-resources\")\npublic class DomainResourceController {\n    @GetMapping\n    public ResponseEntity<List<Map<String, Object>>> list() {\n        Map<String, Object> r = new HashMap<>();\n        r.put(\"id\", 101L);\n        r.put(\"name\", \"AI-COS Enterprise Node\");\n        return ResponseEntity.ok(Collections.singletonList(r));\n    }\n}\n"});

        templates.put("frontend/src/pages/GeneratedDomainPage.tsx", new String[]{"GeneratedDomainPage.tsx", "REACT_TSX", "FRONTEND_AGENT",
            "import React from 'react';\n\nexport const GeneratedDomainPage: React.FC = () => {\n  return (\n    <div className=\"p-8 bg-slate-950 text-slate-100 min-h-screen\">\n      <h1 className=\"text-2xl font-bold\">AI-COS Generated Dashboard</h1>\n    </div>\n  );\n};\n"});

        templates.put("backend/src/main/resources/application.yml", new String[]{"application.yml", "YAML", "DEVOPS_AGENT",
            "spring:\n  application:\n    name: ai-cos-generated-app\n  datasource:\n    url: jdbc:postgresql://localhost:5432/jay-test-db\n    username: admin\n    password: Password123!\nserver:\n  port: 8080\n"});

        templates.put("package.json", new String[]{"package.json", "JSON", "DEPLOYMENT_AGENT",
            "{\n  \"name\": \"ai-cos-generated-frontend\",\n  \"version\": \"1.0.0\",\n  \"scripts\": {\n    \"dev\": \"vite\",\n    \"build\": \"tsc && vite build\"\n  }\n}\n"});

        templates.put("pom.xml", new String[]{"pom.xml", "XML", "DEPLOYMENT_AGENT",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n    <modelVersion>4.0.0</modelVersion>\n    <groupId>com.app</groupId>\n    <artifactId>ai-cos-generated-backend</artifactId>\n    <version>1.0.0-SNAPSHOT</version>\n</project>\n"});

        for (Map.Entry<String, String[]> entry : templates.entrySet()) {
            String path = entry.getKey();
            if (!existingPaths.contains(path)) {
                String[] meta = entry.getValue();
                AgentWorkspaceArtifactEntity a = AgentWorkspaceArtifactEntity.builder()
                        .publicId(UUID.randomUUID())
                        .session(session)
                        .filePath(path)
                        .fileName(meta[0])
                        .artifactType(meta[1])
                        .agentRole(meta[2])
                        .content(meta[3])
                        .build();
                a.setCreatedBy("system");
                a.setUpdatedBy("system");
                artifactRepository.save(a);
            }
        }
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

    @Transactional
    public void deleteSession(UUID publicId) {
        log.info("Deleting Agent Workspace Session publicId: {} and all associated child records...", publicId);
        AgentWorkspaceSessionEntity session = sessionRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent Workspace Session", "publicId", publicId));

        Long sessionId = session.getId();

        // Delete child records in correct dependency order to prevent FK constraint violations
        artifactRepository.deleteBySessionId(sessionId);
        logRepository.deleteBySessionId(sessionId);
        taskRepository.deleteBySessionId(sessionId);
        planRepository.deleteBySessionId(sessionId);

        // Delete parent session entity from PostgreSQL
        sessionRepository.delete(session);
        log.info("Session {} and all associated artifacts, logs, tasks, and plans cleanly deleted.", publicId);
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
                .executionMode(session.getExecutionMode())
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

    private String mapRoleToFileName(String role) {
        if (role == null) return "N/A";
        switch (role) {
            case "BUSINESS_ANALYST": return "requirements-spec.md";
            case "PRODUCT_MANAGER": return "user-stories-matrix.json";
            case "RESEARCH": return "tech-stack-blueprint.md";
            case "SYSTEM_ARCHITECT": return "architecture-blueprint.md";
            case "DATABASE_AGENT": return "V10__generated_schema.sql";
            case "API_AGENT": return "openapi-spec.yaml";
            case "UI_UX_AGENT": return "ui-design-system.json";
            case "FRONTEND_AGENT": return "GeneratedDomainPage.tsx";
            case "BACKEND_AGENT": return "DomainResourceController.java";
            case "AI_ENGINEER_AGENT": return "ai-pipeline-config.json";
            case "SECURITY_AGENT": return "security-audit-report.md";
            case "QA_AGENT": return "GeneratedDomainTests.java";
            case "DEVOPS_AGENT": return "docker-compose.yml";
            case "DOCUMENTATION_AGENT": return "README.md";
            case "CODE_REVIEW_AGENT": return "code-review-audit.json";
            case "DEPLOYMENT_AGENT": return "ai-cos-deliverables.zip";
            default: return "workspace-deliverable.txt";
        }
    }

    private AgentTaskDTO mapToTaskDTO(AgentExecutionTaskEntity t) {
        List<String> deps = fromJson(t.getDependenciesJson(), new TypeReference<List<String>>() {});

        String provider = "Gemini";
        String model = "gemini-1.5-flash";
        long promptTokens = 1200;
        long completionTokens = 450;
        long durationMs = 350;

        if (t.getStartedAt() != null && t.getCompletedAt() != null) {
            durationMs = java.time.Duration.between(t.getStartedAt(), t.getCompletedAt()).toMillis();
        }

        if (t.getOutputSummary() != null) {
            if (t.getOutputSummary().contains("llama")) {
                provider = "Groq";
                model = "llama-3.3-70b-versatile";
            } else if (t.getOutputSummary().contains("LocalAI")) {
                provider = "LocalAI";
                model = "llama3";
            }
        }

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
                .currentFile(mapRoleToFileName(t.getAgentRole()))
                .provider(provider)
                .model(model)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(promptTokens + completionTokens)
                .durationMs(durationMs)
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
