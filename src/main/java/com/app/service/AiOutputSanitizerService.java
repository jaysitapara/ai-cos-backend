package com.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Phase 7.2 — Production-Grade Artifact Sanitization Engine
 *
 * Responsibilities:
 *  1. Strip all AI provider metadata headers and context section markers
 *  2. Extract code from markdown code fences (single or multiple blocks)
 *  3. Apply post-processing: normalize line endings, collapse blank lines, trim trailing spaces
 *  4. Validate artifact content per file type (Java, TSX, SQL, YAML, JSON, MD, Dockerfile, XML, Properties)
 *
 * This service is the LAST gate before any content is persisted to the database or included in a ZIP export.
 * The database must NEVER store raw provider response, prompt context, reasoning, or metadata.
 */
@Slf4j
@Service
public class AiOutputSanitizerService {

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    // ─── Regex: markdown code fence (captures optional lang tag + content) ────
    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile(
        "```[a-zA-Z0-9_+\\-.]*\\r?\\n([\\s\\S]*?)```",
        Pattern.MULTILINE
    );

    // ─── Provider metadata header patterns ────────────────────────────────────
    private static final List<Pattern> METADATA_PATTERNS = List.of(
        // Provider response wrappers
        Pattern.compile("^\\[LocalAI[^\\]]*\\]\\s*:?\\s*", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\[Gemini[^\\]]*\\]\\s*:?\\s*", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\[Groq[^\\]]*\\]\\s*:?\\s*", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\[OpenAI[^\\]]*\\]\\s*:?\\s*", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\[Anthropic[^\\]]*\\]\\s*:?\\s*", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),

        // Prompt context section dividers
        Pattern.compile("^={3,}\\s*WORKSPACE SESSION CONTEXT\\s*={3,}.*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
        Pattern.compile("^={3,}\\s*SYSTEM PROMPT\\s*={3,}.*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
        Pattern.compile("^={3,}\\s*USER PROMPT\\s*={3,}.*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
        Pattern.compile("^={3,}\\s*AGENT INSTRUCTION\\s*={3,}.*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
        Pattern.compile("^={3,}\\s*PREVIOUS AGENT OUTPUTS[^\\n]*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),

        // Prompt context key-value lines that get echoed back
        Pattern.compile("^Goal Prompt:\\s*.*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
        Pattern.compile("^Project Type:\\s*.*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
        Pattern.compile("^Complexity:\\s*.*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
        Pattern.compile("^Task Title:\\s*.*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
        Pattern.compile("^Target Objective:\\s*.*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
        Pattern.compile("^Artifact:\\s*.*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),

        // Conversational preambles
        Pattern.compile("^(Here is (your|the)|Here's (your|the))\\s+.*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
        Pattern.compile("^AI Response:\\s*", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
        Pattern.compile("^A:\\s*", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
        Pattern.compile("^Processed prompt:\\s*", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),

        // Chain-of-thought / reasoning markers
        Pattern.compile("^Thinking\\.{0,3}\\s*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
        Pattern.compile("^Reasoning\\.{0,3}\\s*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
        Pattern.compile("^<thinking>[\\s\\S]*?</thinking>", Pattern.CASE_INSENSITIVE),

        // Token/latency metrics lines
        Pattern.compile("^Token Usage:?.*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
        Pattern.compile("^Latency:?\\s*\\d+.*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\[Context truncated[^\\]]*\\].*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE)
    );

    // ─── PUBLIC API ───────────────────────────────────────────────────────────

    /**
     * Full sanitization pipeline:
     *  1. Extract code from fenced blocks (if any fences present)
     *  2. Strip provider metadata headers
     *  3. Post-process: normalize endings, collapse blanks, trim trailing spaces
     *
     * @param rawOutput Raw string from any AI provider
     * @return Clean source-code-only string, UTF-8 compatible
     */
    public String sanitizeOutput(String rawOutput) {
        if (rawOutput == null || rawOutput.isBlank()) {
            return "";
        }

        String result = rawOutput;

        // Step 1: Extract code from markdown fences if present
        result = extractFromCodeFences(result);

        // Step 2: Strip provider metadata headers and context sections
        result = stripProviderMetadata(result);

        // Step 3: Post-processing normalization
        result = postProcess(result);

        log.debug("Sanitized output: original {} chars → {} chars", rawOutput.length(), result.length());
        return result;
    }

    /**
     * Validates artifact content is structurally appropriate for its declared type.
     *
     * @param content       Sanitized content string
     * @param artifactType  One of: JAVA_SOURCE, REACT_TSX, DATABASE_DDL, SWAGGER_SPEC,
     *                      YAML, DOCKER, MARKDOWN, JSON, XML, PROPERTIES, DOCKERFILE
     * @return true if content passes structural validation
     */
    public boolean validateArtifactContent(String content, String artifactType) {
        if (content == null || content.isBlank()) {
            log.warn("Validation failed: content is null or blank for type '{}'", artifactType);
            return false;
        }

        String trimmed = content.trim();
        String upper = trimmed.toUpperCase();

        try {
            switch (artifactType) {

                case "JAVA_SOURCE":
                    return trimmed.startsWith("package ")
                        || trimmed.contains("import ")
                        || trimmed.contains("public class ")
                        || trimmed.contains("public interface ")
                        || trimmed.contains("public enum ")
                        || trimmed.contains("public @interface ");

                case "REACT_TSX":
                case "TYPESCRIPT":
                    return (trimmed.contains("import React") || trimmed.contains("from 'react'") || trimmed.contains("from \"react\""))
                        && (trimmed.contains("export ") || trimmed.contains("return (") || trimmed.contains("return("));

                case "DATABASE_DDL":
                case "SQL":
                    return upper.contains("CREATE TABLE")
                        || upper.contains("ALTER TABLE")
                        || upper.contains("INSERT INTO")
                        || upper.contains("CREATE INDEX")
                        || upper.contains("CREATE SEQUENCE");

                case "SWAGGER_SPEC":
                    yamlMapper.readTree(trimmed);
                    return trimmed.contains("openapi:") || trimmed.contains("swagger:");

                case "YAML":
                    yamlMapper.readTree(trimmed);
                    return true;

                case "DOCKER":
                    yamlMapper.readTree(trimmed);
                    return trimmed.contains("services:") || trimmed.contains("image:") || trimmed.contains("version:");

                case "DOCKERFILE":
                    // First non-blank, non-comment line must be FROM
                    for (String line : trimmed.split("\\r?\\n")) {
                        String stripped = line.strip();
                        if (!stripped.isEmpty() && !stripped.startsWith("#")) {
                            return stripped.toUpperCase().startsWith("FROM ");
                        }
                    }
                    return false;

                case "MARKDOWN":
                    return trimmed.contains("# ")
                        || trimmed.startsWith("#")
                        || trimmed.contains("\n# ")
                        || trimmed.contains("\n## ");

                case "JSON":
                    jsonMapper.readTree(trimmed);
                    return true;

                case "XML":
                    return trimmed.startsWith("<?xml") || trimmed.startsWith("<project") || trimmed.startsWith("<");

                case "PROPERTIES":
                    return trimmed.contains("=") || trimmed.contains(":");

                default:
                    // Unknown types pass through — do not block persistence
                    return true;
            }
        } catch (Exception e) {
            log.warn("Validation exception for artifact type '{}': {}", artifactType, e.getMessage());
            return false;
        }
    }

    // ─── PRIVATE PIPELINE STEPS ───────────────────────────────────────────────

    /**
     * Extracts code from markdown code fences.
     * If one or more fenced blocks are found, concatenates their inner content.
     * If no fences found, returns input unchanged (raw text output from model).
     */
    private String extractFromCodeFences(String input) {
        Matcher matcher = CODE_FENCE_PATTERN.matcher(input);
        List<String> blocks = new ArrayList<>();
        while (matcher.find()) {
            String block = matcher.group(1);
            if (block != null && !block.isBlank()) {
                blocks.add(block.stripTrailing());
            }
        }

        if (!blocks.isEmpty()) {
            log.debug("Extracted {} code block(s) from fenced markdown", blocks.size());
            return String.join("\n", blocks);
        }

        // No fences present — return as-is for metadata stripping
        return input;
    }

    /**
     * Removes all provider metadata headers, context section markers, and
     * conversational preambles line by line.
     */
    private String stripProviderMetadata(String input) {
        String result = input;
        for (Pattern pattern : METADATA_PATTERNS) {
            result = pattern.matcher(result).replaceAll("");
        }
        return result;
    }

    /**
     * Post-processing normalization:
     *  - CRLF → LF
     *  - Trailing spaces stripped per line
     *  - Max 1 consecutive blank line (collapses 2+ blank lines)
     *  - Leading and trailing whitespace trimmed
     */
    private String postProcess(String input) {
        // Normalize line endings
        String result = input.replace("\r\n", "\n").replace("\r", "\n");

        // Process line by line
        String[] lines = result.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        int consecutiveBlanks = 0;

        for (String line : lines) {
            String trimmedLine = line.stripTrailing();
            if (trimmedLine.isEmpty()) {
                consecutiveBlanks++;
                if (consecutiveBlanks <= 1) {
                    sb.append("\n");
                }
                // Suppress if already have 1 blank line
            } else {
                consecutiveBlanks = 0;
                sb.append(trimmedLine).append("\n");
            }
        }

        return sb.toString().strip();
    }
}
