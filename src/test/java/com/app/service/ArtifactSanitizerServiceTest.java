package com.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 7.2 — ArtifactSanitizerService Unit Tests
 *
 * Covers all 10 responsibilities from the Phase 7.2 spec:
 *  1. Multi-fence code extraction
 *  2. Provider metadata stripping
 *  3. Context section header removal
 *  4. Post-processing normalization
 *  5. Per-artifact-type validation
 *  6. Before vs After end-to-end verification
 *
 * All tests run WITHOUT Spring context (pure unit tests).
 */
@DisplayName("ArtifactSanitizerService — Phase 7.2")
class ArtifactSanitizerServiceTest {

    private AiOutputSanitizerService sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new AiOutputSanitizerService();
    }

    // ─── 1. Code Fence Extraction ──────────────────────────────────────────────

    @Test
    @DisplayName("Should extract Java code from a single ```java fence")
    void testExtractsFromSingleJavaCodeFence() {
        String raw = "```java\npackage com.app.controller;\n\npublic class Foo {}\n```";
        String result = sanitizer.sanitizeOutput(raw);
        assertEquals("package com.app.controller;\n\npublic class Foo {}", result);
    }

    @Test
    @DisplayName("Should extract TSX code from a ```tsx fence")
    void testExtractsFromTsxCodeFence() {
        String raw = "Here is your component:\n```tsx\nimport React from 'react';\nexport const Page = () => <div/>;```";
        String result = sanitizer.sanitizeOutput(raw);
        assertTrue(result.contains("import React"), "Expected React import. Got: " + result);
        assertFalse(result.contains("Here is your component"), "Should not contain preamble");
    }

    @Test
    @DisplayName("Should extract SQL code from a ```sql fence")
    void testExtractsFromSqlCodeFence() {
        String raw = "```sql\nCREATE TABLE users (\n    id BIGSERIAL PRIMARY KEY\n);\n```";
        String result = sanitizer.sanitizeOutput(raw);
        assertTrue(result.contains("CREATE TABLE"), "Expected SQL DDL. Got: " + result);
        assertFalse(result.contains("```"), "Should not contain backticks");
    }

    @Test
    @DisplayName("Should extract YAML code from a ```yaml fence")
    void testExtractsFromYamlCodeFence() {
        String raw = "Here is your docker-compose:\n```yaml\nservices:\n  backend:\n    image: myapp:latest\n```";
        String result = sanitizer.sanitizeOutput(raw);
        assertTrue(result.contains("services:"), "Expected YAML content. Got: " + result);
        assertFalse(result.contains("```"), "Should not contain backticks");
    }

    @Test
    @DisplayName("Should extract first code block when multiple fences present")
    void testExtractsFirstBlockFromMultipleCodeFences() {
        String raw = "Explanation:\n```java\npackage com.app;\npublic class A {}\n```\nMore text:\n```java\npublic class B {}\n```";
        String result = sanitizer.sanitizeOutput(raw);
        assertTrue(result.contains("package com.app"), "Expected first block. Got: " + result);
        assertFalse(result.contains("Explanation"), "Should not contain prose explanation");
    }

    @Test
    @DisplayName("Should handle bare ``` code fences without language tag")
    void testExtractsBareCodeFence() {
        String raw = "```\nFROM eclipse-temurin:17-jdk-alpine\nWORKDIR /app\n```";
        String result = sanitizer.sanitizeOutput(raw);
        assertTrue(result.contains("FROM eclipse-temurin"), "Expected Dockerfile content. Got: " + result);
        assertFalse(result.contains("```"), "Should not contain backtick fences");
    }

    // ─── 2. Provider Metadata Stripping ───────────────────────────────────────

    @Test
    @DisplayName("Should strip [OpenAI ...] header from Java source")
    void testStripsOpenAiHeaderFromJava() {
        String raw = "[OpenAI gpt-4o Response]: Processed prompt:\n" +
                     "=== WORKSPACE SESSION CONTEXT ===\n" +
                     "Goal Prompt: Build a Todo App\n" +
                     "Project Type: Enterprise\n\n" +
                     "package com.app.controller;\n\npublic class TodoController {}";

        String result = sanitizer.sanitizeOutput(raw);

        assertFalse(result.contains("[OpenAI"), "OpenAI header must be stripped");
        assertFalse(result.contains("=== WORKSPACE SESSION CONTEXT ==="), "Context section must be stripped");
        assertFalse(result.contains("Processed prompt:"), "Processed prompt prefix must be stripped");
        assertFalse(result.contains("Goal Prompt:"), "Goal Prompt line must be stripped");
        assertTrue(result.contains("package com.app.controller;"), "Java package declaration must remain");
    }

    @Test
    @DisplayName("Should strip [Gemini ...] header")
    void testStripsGeminiHeader() {
        String raw = "[Gemini gemini-1.5-flash Response]:\n\nimport React from 'react';\nexport const Page = () => <div/>;";
        String result = sanitizer.sanitizeOutput(raw);
        assertFalse(result.contains("[Gemini"), "Gemini header must be stripped");
        assertTrue(result.contains("import React"), "React import must remain");
    }

    @Test
    @DisplayName("Should strip === section dividers (WORKSPACE SESSION CONTEXT, SYSTEM PROMPT, AGENT INSTRUCTION)")
    void testStripsWorkspaceContextSections() {
        String raw =
            "=== WORKSPACE SESSION CONTEXT ===\n" +
            "Goal Prompt: Build a hospital system\n" +
            "Project Type: Healthcare\n" +
            "Complexity: HIGH\n\n" +
            "=== PREVIOUS AGENT OUTPUTS & DELIVERABLES ===\n" +
            "Artifact: schema.sql\n\n" +
            "=== AGENT INSTRUCTION ===\n" +
            "You are a Spring Boot Engineer.\n\n" +
            "package com.app;\npublic class HospitalController {}";

        String result = sanitizer.sanitizeOutput(raw);

        assertFalse(result.contains("=== WORKSPACE SESSION CONTEXT ==="), "Context header must be stripped");
        assertFalse(result.contains("=== AGENT INSTRUCTION ==="), "Agent instruction header must be stripped");
        assertFalse(result.contains("Goal Prompt:"), "Goal Prompt must be stripped");
        assertTrue(result.contains("package com.app;"), "Java source must remain");
    }

    // ─── 3. Post-Processing ───────────────────────────────────────────────────

    @Test
    @DisplayName("Should normalize CRLF to LF line endings")
    void testNormalizesCrlfToLf() {
        String raw = "package com.app;\r\n\r\npublic class A {}\r\n";
        String result = sanitizer.sanitizeOutput(raw);
        assertFalse(result.contains("\r"), "CRLF must be normalized to LF");
        assertTrue(result.contains("package com.app;"), "Content must remain");
    }

    @Test
    @DisplayName("Should collapse 3+ consecutive blank lines to a single blank line")
    void testCollapsesDuplicateBlankLines() {
        String raw = "line1\n\n\n\n\nline2\n\n\n\nline3";
        String result = sanitizer.sanitizeOutput(raw);
        assertFalse(result.contains("\n\n\n"), "No more than 1 consecutive blank line allowed");
        assertTrue(result.contains("line1"), "line1 must remain");
        assertTrue(result.contains("line2"), "line2 must remain");
        assertTrue(result.contains("line3"), "line3 must remain");
    }

    // ─── 4. Artifact Type Validation ──────────────────────────────────────────

    @Test
    @DisplayName("Should validate Java source — valid input")
    void testValidatesJavaSource() {
        String java = "package com.app.controller;\n\npublic class Ctrl {}";
        assertTrue(sanitizer.validateArtifactContent(java, "JAVA_SOURCE"));
    }

    @Test
    @DisplayName("Should reject Java source — blank content")
    void testRejectsBlankJavaSource() {
        assertFalse(sanitizer.validateArtifactContent("   ", "JAVA_SOURCE"));
        assertFalse(sanitizer.validateArtifactContent(null, "JAVA_SOURCE"));
    }

    @Test
    @DisplayName("Should validate TSX — valid React component")
    void testValidatesTsx() {
        String tsx = "import React from 'react';\nexport const Page: React.FC = () => <div>Hello</div>;";
        assertTrue(sanitizer.validateArtifactContent(tsx, "REACT_TSX"));
    }

    @Test
    @DisplayName("Should reject TSX — missing React import and export")
    void testRejectsInvalidTsx() {
        String notTsx = "Hello world this is not React";
        assertFalse(sanitizer.validateArtifactContent(notTsx, "REACT_TSX"));
    }

    @Test
    @DisplayName("Should validate SQL — CREATE TABLE statement")
    void testValidatesSql() {
        String sql = "CREATE TABLE todos (\n    id BIGSERIAL PRIMARY KEY,\n    title VARCHAR(255) NOT NULL\n);";
        assertTrue(sanitizer.validateArtifactContent(sql, "DATABASE_DDL"));
    }

    @Test
    @DisplayName("Should validate YAML — docker-compose")
    void testValidatesYaml() {
        String yaml = "services:\n  backend:\n    image: myapp:latest\n    ports:\n      - \"8080:8080\"";
        assertTrue(sanitizer.validateArtifactContent(yaml, "DOCKER"));
    }

    @Test
    @DisplayName("Should reject invalid YAML — malformed indentation")
    void testRejectsInvalidYaml() {
        String badYaml = "key:\n  - valid\n    bad: [unclosed";
        assertFalse(sanitizer.validateArtifactContent(badYaml, "YAML"));
    }

    @Test
    @DisplayName("Should validate Markdown — heading present")
    void testValidatesMarkdown() {
        String md = "# My Project\n\n## Overview\nThis is the README.";
        assertTrue(sanitizer.validateArtifactContent(md, "MARKDOWN"));
    }

    @Test
    @DisplayName("Should validate JSON — valid object")
    void testValidatesJson() {
        String json = "{\"name\": \"ai-cos-frontend\", \"version\": \"1.0.0\"}";
        assertTrue(sanitizer.validateArtifactContent(json, "JSON"));
    }

    @Test
    @DisplayName("Should reject JSON — invalid syntax")
    void testRejectsInvalidJson() {
        String badJson = "{name: ai-cos, version: 1.0}"; // missing quotes
        assertFalse(sanitizer.validateArtifactContent(badJson, "JSON"));
    }

    @Test
    @DisplayName("Should validate Dockerfile — FROM as first non-blank non-comment line")
    void testValidatesDockerfile() {
        String dockerfile = "# Builder stage\nFROM eclipse-temurin:17-jdk-alpine AS builder\nWORKDIR /app";
        assertTrue(sanitizer.validateArtifactContent(dockerfile, "DOCKERFILE"));
    }

    @Test
    @DisplayName("Should reject Dockerfile — missing FROM on first non-blank line")
    void testRejectsDockerfileWithoutFrom() {
        String badDockerfile = "WORKDIR /app\nFROM ubuntu:22.04";
        assertFalse(sanitizer.validateArtifactContent(badDockerfile, "DOCKERFILE"));
    }

    // ─── 5. End-to-End Before vs After ────────────────────────────────────────

    @Test
    @DisplayName("E2E: Raw Gemini dump → clean Java source code only")
    void testEndToEndRawGeminiDumpToCleanJava() {
        String before =
            "[Gemini gemini-1.5-flash Response]: Processed prompt:\n" +
            "=== WORKSPACE SESSION CONTEXT ===\n" +
            "Goal Prompt: Build a Todo App using Spring Boot and React\n" +
            "Project Type: Enterprise Web Application System\n" +
            "Complexity: MEDIUM\n\n" +
            "=== PREVIOUS AGENT OUTPUTS & DELIVERABLES ===\n" +
            "Artifact: V10__generated_schema.sql (Role: DATABASE_AGENT)\n" +
            "CREATE TABLE todos (id BIGSERIAL PRIMARY KEY);\n\n" +
            "=== AGENT INSTRUCTION ===\n" +
            "You are a Staff Spring Boot Engineer.\n" +
            "Generate a complete Java Spring Boot 3 REST Controller...\n\n" +
            "```java\n" +
            "package com.app.controller;\n" +
            "\n" +
            "import org.springframework.http.ResponseEntity;\n" +
            "import org.springframework.web.bind.annotation.*;\n" +
            "\n" +
            "@RestController\n" +
            "@RequestMapping(\"/api/v1/todos\")\n" +
            "public class TodoController {\n" +
            "    @GetMapping\n" +
            "    public ResponseEntity<String> list() {\n" +
            "        return ResponseEntity.ok(\"[]\");\n" +
            "    }\n" +
            "}\n" +
            "```";

        String after = sanitizer.sanitizeOutput(before);

        assertFalse(after.contains("[Gemini"), "Gemini header must be gone");
        assertFalse(after.contains("=== WORKSPACE SESSION CONTEXT ==="), "Context section must be gone");
        assertFalse(after.contains("=== AGENT INSTRUCTION ==="), "Agent instruction must be gone");
        assertFalse(after.contains("Goal Prompt:"), "Goal Prompt must be gone");
        assertFalse(after.contains("Processed prompt:"), "Processed prompt must be gone");
        assertFalse(after.contains("```"), "Code fences must be gone");

        // Assert: valid Java source code present
        assertTrue(after.contains("package com.app.controller;"), "Package declaration must be present");
        assertTrue(after.contains("@RestController"), "Spring annotation must be present");
        assertTrue(after.contains("public class TodoController"), "Class declaration must be present");

        // Assert: validates as JAVA_SOURCE
        assertTrue(sanitizer.validateArtifactContent(after, "JAVA_SOURCE"),
                   "Sanitized content must pass JAVA_SOURCE validation");
    }
}
