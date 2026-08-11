package com.app.provider.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Phase 7.2 — Real Anthropic Messages API Implementation
 *
 * Calls POST https://api.anthropic.com/v1/messages using the claude-3-5-sonnet model.
 * Returns only the content[0].text field from the response.
 *
 * IMPORTANT: This provider no longer echoes the prompt as the response.
 * It returns only the model's generated text.
 */
@Slf4j
@Component
public class AnthropicProviderImpl implements AiProvider {

    private static final String ANTHROPIC_API_VERSION = "2023-06-01";

    @Value("${ai.providers.anthropic.api-key:}")
    private String apiKey;

    @Value("${ai.providers.anthropic.model:claude-3-5-sonnet-20241022}")
    private String defaultModel;

    private final RestClient restClient;

    public AnthropicProviderImpl() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.anthropic.com/v1")
                .build();
    }

    @Override
    public String getProviderName() {
        return "Anthropic";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public AiCompletionResponse generateCompletion(AiCompletionRequest request) {
        if (!isAvailable()) {
            throw new IllegalStateException("Anthropic API key is not configured");
        }

        String modelToUse = request.getModel() != null ? request.getModel() : defaultModel;
        log.info("Sending live HTTP request to Anthropic Messages API using model: {}", modelToUse);

        Map<String, Object> body = Map.of(
            "model", modelToUse,
            "max_tokens", request.getMaxTokens() > 0 ? request.getMaxTokens() : 2048,
            "messages", List.of(Map.of("role", "user", "content", request.getPrompt()))
        );

        try {
            AnthropicResponse response = restClient.post()
                .uri("/messages")
                .header("x-api-key", apiKey.trim())
                .header("anthropic-version", ANTHROPIC_API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(AnthropicResponse.class);

            if (response != null && response.getContent() != null && !response.getContent().isEmpty()) {
                String text = response.getContent().stream()
                    .filter(c -> "text".equals(c.getType()) && c.getText() != null)
                    .map(AnthropicContentBlock::getText)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No text content in Anthropic response"));

                String finishReason = response.getStopReason() != null ? response.getStopReason() : "end_turn";
                int inputTokens = response.getUsage() != null ? response.getUsage().getInputTokens() : 0;
                int outputTokens = response.getUsage() != null ? response.getUsage().getOutputTokens() : 0;

                log.info("Anthropic response received: {} tokens (input={}, output={})",
                         inputTokens + outputTokens, inputTokens, outputTokens);
                return new AiCompletionResponse(text, modelToUse, finishReason, inputTokens, outputTokens);
            }

            throw new RuntimeException("Empty response received from Anthropic API");

        } catch (Exception e) {
            log.error("Failed to execute live completion request against Anthropic API: {}", e.getMessage());
            throw new RuntimeException("Anthropic API Execution Failed: " + e.getMessage(), e);
        }
    }

    // ─── Response DTOs ─────────────────────────────────────────────────────────

    @Data
    private static class AnthropicResponse {
        private List<AnthropicContentBlock> content;
        @JsonProperty("stop_reason")
        private String stopReason;
        private AnthropicUsage usage;
    }

    @Data
    private static class AnthropicContentBlock {
        private String type;
        private String text;
    }

    @Data
    private static class AnthropicUsage {
        @JsonProperty("input_tokens")
        private int inputTokens;
        @JsonProperty("output_tokens")
        private int outputTokens;
    }
}
