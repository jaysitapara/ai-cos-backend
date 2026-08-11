package com.app.service;

import org.springframework.stereotype.Service;

/**
 * Phase 7.2 — Agent Prompt Engine with Strict Output Preamble Injection
 *
 * Every artifact-producing agent prompt now begins with a CRITICAL INSTRUCTIONS block
 * that explicitly forbids the LLM from:
 *  - Wrapping output in markdown code fences
 *  - Including any explanations, reasoning, or commentary
 *  - Echoing provider name, token usage, or latency
 *  - Repeating prompt context or system sections
 *
 * This is the FIRST layer of the sanitization pipeline — coaxing the LLM to produce
 * clean output BEFORE it even reaches AiOutputSanitizerService.
 */
@Service
public class AgentPromptEngine {

    /**
     * Strict output preamble injected at the START of every artifact-generating agent prompt.
     * The MODEL sees this first and must comply before generating any content.
     */
    private static final String STRICT_OUTPUT_PREAMBLE =
        "CRITICAL INSTRUCTIONS — READ BEFORE RESPONDING:\n" +
        "Return ONLY the requested file content. Nothing else.\n" +
        "Do NOT wrap the output in markdown code fences (no ``` markers of any kind).\n" +
        "Do NOT include \"Here is your code\", \"Here is the file\", or any similar preamble.\n" +
        "Do NOT include explanations, commentary, reasoning, or descriptions.\n" +
        "Do NOT include your provider name, token usage, latency, or any metadata.\n" +
        "Do NOT repeat the prompt text, goal, context sections, or system instructions.\n" +
        "Do NOT include Thinking..., Reasoning..., or chain-of-thought markers.\n" +
        "Return ONLY the raw file content, starting from the very first line of the file.\n" +
        "─────────────────────────────────────────────────────────────────────────────\n\n";

    public String buildPromptForAgent(String agentRole, String taskTitle, String goalPrompt, String projectType) {
        String baseContext =
            "Target Objective: " + goalPrompt + "\n" +
            "Project Type: " + projectType + "\n" +
            "Task Title: " + taskTitle + "\n\n";

        switch (agentRole) {

            case "DATABASE_AGENT":
                return STRICT_OUTPUT_PREAMBLE + baseContext +
                    "You are a Principal Database Architect.\n" +
                    "Generate production-ready PostgreSQL Flyway SQL schema DDL for this project.\n" +
                    "Include CREATE TABLE statements with PRIMARY KEY, FOREIGN KEY, NOT NULL constraints,\n" +
                    "indexes, TIMESTAMPTZ timestamps, and meaningful seed data if appropriate.\n" +
                    "Output ONLY valid SQL starting with a comment header like '-- Schema for <project>'.\n" +
                    "No markdown. No explanations. No code fences. Just the SQL.";

            case "API_AGENT":
                return STRICT_OUTPUT_PREAMBLE + baseContext +
                    "You are a Senior API Engineer.\n" +
                    "Generate a complete OpenAPI 3.0 specification in YAML format for the core REST APIs.\n" +
                    "Include: info section, servers, paths (GET/POST/PUT/DELETE), requestBody, responses,\n" +
                    "components/schemas, and security schemes (BearerAuth JWT).\n" +
                    "Output ONLY valid YAML starting with 'openapi: 3.0.3'.\n" +
                    "No markdown. No explanations. No code fences. Just the YAML.";

            case "BACKEND_AGENT":
                return STRICT_OUTPUT_PREAMBLE + baseContext +
                    "You are a Staff Spring Boot Engineer.\n" +
                    "Generate a complete Java Spring Boot 3 REST Controller class for the core domain resource.\n" +
                    "Use @RestController, @RequestMapping(\"/api/v1/resource\"), @GetMapping, @PostMapping,\n" +
                    "@PutMapping, @DeleteMapping, @PathVariable, @RequestBody, and ResponseEntity.\n" +
                    "Include proper Lombok annotations (@RequiredArgsConstructor), service injection, and Javadoc.\n" +
                    "Output ONLY valid Java source starting with 'package com.app.controller;'.\n" +
                    "No markdown. No explanations. No code fences. Just the Java source code.";

            case "FRONTEND_AGENT":
                return STRICT_OUTPUT_PREAMBLE + baseContext +
                    "You are a Lead React TypeScript Engineer.\n" +
                    "Generate a full React 18 TypeScript component (TSX) for the domain dashboard page.\n" +
                    "Use Tailwind CSS utility classes and Lucide React icons.\n" +
                    "Include: useState, useEffect for data fetching, a loading skeleton, error state,\n" +
                    "and a data table or card grid for the main resource list.\n" +
                    "Output ONLY valid TSX starting with 'import React from 'react';'.\n" +
                    "No markdown. No explanations. No code fences. Just the TSX source code.";

            case "DEVOPS_AGENT":
                return STRICT_OUTPUT_PREAMBLE + baseContext +
                    "You are a Principal DevOps Engineer.\n" +
                    "Generate a production-ready docker-compose.yml for containerizing:\n" +
                    "  - backend (Spring Boot on port 8080)\n" +
                    "  - frontend (React/Nginx on port 80)\n" +
                    "  - database (PostgreSQL 16 on port 5432)\n" +
                    "Include health checks, environment variables, named volumes, and a custom network.\n" +
                    "Output ONLY valid YAML starting with 'services:'.\n" +
                    "No markdown. No explanations. No code fences. Just the YAML.";

            case "DOCUMENTATION_AGENT":
                return STRICT_OUTPUT_PREAMBLE + baseContext +
                    "You are a Technical Writer & Principal Solution Architect.\n" +
                    "Generate a comprehensive system README.md covering:\n" +
                    "  - Project overview & executive summary\n" +
                    "  - Architecture diagram (Mermaid graph)\n" +
                    "  - Tech stack table\n" +
                    "  - Local development setup (Docker & manual steps)\n" +
                    "  - Environment variable reference\n" +
                    "  - Key API endpoints with curl examples\n" +
                    "  - Deployment instructions\n" +
                    "Output ONLY clean Markdown starting with '# <Project Name>'.\n" +
                    "No code fences around the entire document. No provider headers. Just the Markdown.";

            case "SYSTEM_ARCHITECT":
                return STRICT_OUTPUT_PREAMBLE + baseContext +
                    "You are a Principal Systems Architect.\n" +
                    "Generate a comprehensive system architecture document in Markdown.\n" +
                    "Include: component diagram (Mermaid), ER diagram (Mermaid), data flow,\n" +
                    "technology decisions with rationale, and cross-cutting concerns (security, observability).\n" +
                    "Output ONLY clean Markdown starting with '# System Architecture Blueprint'.\n" +
                    "No code fences around the document. Just the Markdown.";

            case "BUSINESS_ANALYST":
                return STRICT_OUTPUT_PREAMBLE + baseContext +
                    "You are a Senior Business Analyst.\n" +
                    "Generate a detailed Business Requirements Document (BRD) in Markdown.\n" +
                    "Include: executive summary, stakeholder analysis, functional scope,\n" +
                    "user personas, operational objectives, and a MOSCOW prioritization matrix.\n" +
                    "Output ONLY clean Markdown starting with '# Business Requirements Document'.\n" +
                    "No code fences around the document. Just the Markdown.";

            case "PRODUCT_MANAGER":
                return STRICT_OUTPUT_PREAMBLE + baseContext +
                    "You are a Principal Product Manager.\n" +
                    "Generate a Product Requirements Document (PRD) in Markdown.\n" +
                    "Include: product vision, user stories (As a... I want... So that...),\n" +
                    "acceptance criteria, feature priority matrix, and success metrics.\n" +
                    "Output ONLY clean Markdown starting with '# Product Requirements Document'.\n" +
                    "No code fences around the document. Just the Markdown.";

            case "RESEARCH":
                return STRICT_OUTPUT_PREAMBLE + baseContext +
                    "You are a Principal Technology Researcher.\n" +
                    "Generate a Technology Stack Blueprint document in Markdown.\n" +
                    "Include: recommended technologies with version numbers, trade-off comparisons,\n" +
                    "integration patterns, and a recommended project structure.\n" +
                    "Output ONLY clean Markdown starting with '# Technology Stack Blueprint'.\n" +
                    "No code fences around the document. Just the Markdown.";

            case "UI_UX_AGENT":
                return STRICT_OUTPUT_PREAMBLE + baseContext +
                    "You are a Lead UX Designer.\n" +
                    "Generate a design system specification as a JSON object.\n" +
                    "Include: colors (primary, secondary, accent, neutral, semantic), typography\n" +
                    "(font families, scale), spacing scale, border radii, shadow tokens, and component variants.\n" +
                    "Output ONLY valid JSON starting with '{'.\n" +
                    "No markdown. No explanations. No code fences. Just the JSON.";

            case "AI_ENGINEER_AGENT":
                return STRICT_OUTPUT_PREAMBLE + baseContext +
                    "You are a Principal AI/ML Engineer.\n" +
                    "Generate a production AI pipeline configuration YAML for integrating AI capabilities.\n" +
                    "Include: model registry config, inference endpoints, prompt templates, RAG pipeline,\n" +
                    "safety guardrails, rate limiting, and observability hooks.\n" +
                    "Output ONLY valid YAML starting with 'ai-pipeline:'.\n" +
                    "No markdown. No explanations. No code fences. Just the YAML.";

            case "SECURITY_AGENT":
                return STRICT_OUTPUT_PREAMBLE + baseContext +
                    "You are a Principal Security Engineer (CISSP, OWASP).\n" +
                    "Generate a Security Audit Report in Markdown.\n" +
                    "Include: OWASP Top 10 assessment, authentication & authorization review,\n" +
                    "RBAC matrix, data encryption posture, vulnerability findings, and remediation steps.\n" +
                    "Output ONLY clean Markdown starting with '# Security Audit Report'.\n" +
                    "No code fences around the document. Just the Markdown.";

            case "QA_AGENT":
                return STRICT_OUTPUT_PREAMBLE + baseContext +
                    "You are a Lead Quality Assurance Engineer.\n" +
                    "Generate a comprehensive JUnit 5 + Mockito test class for the core backend service.\n" +
                    "Include: @ExtendWith(MockitoExtension.class), @Mock dependencies, @InjectMocks subject,\n" +
                    "@BeforeEach setup, and at least 5 @Test methods covering happy path, edge cases, and error conditions.\n" +
                    "Output ONLY valid Java source starting with 'package com.app.service;'.\n" +
                    "No markdown. No explanations. No code fences. Just the Java source code.";

            case "CODE_REVIEW_AGENT":
                return STRICT_OUTPUT_PREAMBLE + baseContext +
                    "You are a Principal Software Engineer (Code Reviewer).\n" +
                    "Generate a Static Code Quality Audit Report in Markdown.\n" +
                    "Include: code quality score (0-100), findings table (file, severity, issue, recommendation),\n" +
                    "architectural concerns, test coverage assessment, and an actionable improvement roadmap.\n" +
                    "Output ONLY clean Markdown starting with '# Static Code Quality Audit Report'.\n" +
                    "No code fences around the document. Just the Markdown.";

            case "DEPLOYMENT_AGENT":
                return STRICT_OUTPUT_PREAMBLE + baseContext +
                    "You are a Principal DevOps / SRE Engineer.\n" +
                    "Generate a Production Deployment Runbook in Markdown.\n" +
                    "Include: pre-deployment checklist, deployment steps (with exact commands),\n" +
                    "health check verification, rollback procedure, monitoring setup, and post-deployment sign-off.\n" +
                    "Output ONLY clean Markdown starting with '# Production Deployment Runbook'.\n" +
                    "No code fences around the document. Just the Markdown.";

            default:
                return STRICT_OUTPUT_PREAMBLE + baseContext +
                    "You are a specialized AI Agent (" + agentRole + ") on the AI-COS platform.\n" +
                    "Execute detailed reasoning and deliver a production-ready Markdown report for: " + taskTitle + ".\n" +
                    "Output ONLY clean Markdown. No code fences around the document. No provider headers.";
        }
    }
}
