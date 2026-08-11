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
 * Phase 7.2 — Real Ollama/LocalAI HTTP Provider Implementation
 *
 * Connects to a locally running Ollama or LocalAI server using the
 * OpenAI-compatible REST API (/v1/chat/completions).
 *
 * If the endpoint is unreachable or returns an error, a RuntimeException is thrown
 * so that AiProviderFactory.generateCompletionWithFallback() can try the next provider.
 *
 * IMPORTANT: This provider no longer echoes the prompt as the response.
 * It returns only the model's message.content field.
 */
@Slf4j
@Component
public class LocalAiProviderImpl implements AiProvider {

    @Value("${ai.providers.local.endpoint:http://localhost:11434}")
    private String endpoint;

    @Value("${ai.providers.local.model:llama3}")
    private String defaultModel;

    @Override
    public String getProviderName() {
        return "LocalAI";
    }

    @Override
    public boolean isAvailable() {
        return endpoint != null && !endpoint.trim().isEmpty();
    }

    @Override
    public AiCompletionResponse generateCompletion(AiCompletionRequest request) {
        if (!isAvailable()) {
            throw new IllegalStateException("LocalAI endpoint is not configured");
        }

        String modelToUse = request.getModel() != null ? request.getModel() : defaultModel;
        log.info("Sending live HTTP request to LocalAI/Ollama at {} using model: {}", endpoint, modelToUse);

        RestClient restClient = RestClient.builder()
                .baseUrl(endpoint.trim())
                .build();

        Map<String, Object> body = Map.of(
            "model", modelToUse,
            "messages", List.of(Map.of("role", "user", "content", request.getPrompt())),
            "temperature", request.getTemperature() != null ? request.getTemperature() : 0.2,
            "stream", false
        );

        try {
            LocalAiResponse response = restClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(LocalAiResponse.class);

            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                String text = response.getChoices().get(0).getMessage().getContent();
                String finishReason = response.getChoices().get(0).getFinishReason();
                int promptTokens = response.getUsage() != null ? response.getUsage().getPromptTokens() : 0;
                int completionTokens = response.getUsage() != null ? response.getUsage().getCompletionTokens() : 0;

                log.info("LocalAI response received: {} tokens (prompt={}, completion={})",
                         promptTokens + completionTokens, promptTokens, completionTokens);
                return new AiCompletionResponse(text, modelToUse, finishReason, promptTokens, completionTokens);
            }

            throw new RuntimeException("Empty response received from LocalAI at " + endpoint);

        } catch (Exception e) {
            log.error("Failed to execute completion request against LocalAI endpoint '{}': {}", endpoint, e.getMessage());
            throw new RuntimeException("LocalAI Execution Failed: " + e.getMessage(), e);
        }
    }

    // ─── Response DTOs ─────────────────────────────────────────────────────────

    @Data
    private static class LocalAiResponse {
        private List<LocalAiChoice> choices;
        private LocalAiUsage usage;
    }

    @Data
    private static class LocalAiChoice {
        private LocalAiMessage message;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    private static class LocalAiMessage {
        private String content;
    }

    @Data
    private static class LocalAiUsage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        @JsonProperty("completion_tokens")
        private int completionTokens;
    }
}
