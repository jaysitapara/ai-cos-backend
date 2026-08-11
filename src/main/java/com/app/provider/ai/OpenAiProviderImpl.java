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
 * Phase 7.2 — Real OpenAI Chat Completions API Implementation
 *
 * Calls POST https://api.openai.com/v1/chat/completions using the gpt-4o model.
 * Returns only the message.content field from the first choice.
 *
 * IMPORTANT: This provider no longer echoes the prompt as the response.
 * It returns only the model's generated text.
 */
@Slf4j
@Component
public class OpenAiProviderImpl implements AiProvider {

    @Value("${ai.providers.openai.api-key:${OPENAI_API_KEY:}}")
    private String apiKey;

    @Value("${ai.providers.openai.model:gpt-4o}")
    private String defaultModel;

    private final RestClient restClient;

    public OpenAiProviderImpl() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .build();
    }

    @Override
    public String getProviderName() {
        return "OpenAI";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public AiCompletionResponse generateCompletion(AiCompletionRequest request) {
        if (!isAvailable()) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }

        String modelToUse = request.getModel() != null ? request.getModel() : defaultModel;
        log.info("Sending live HTTP request to OpenAI API using model: {}", modelToUse);

        Map<String, Object> body = Map.of(
            "model", modelToUse,
            "messages", List.of(Map.of("role", "user", "content", request.getPrompt())),
            "temperature", request.getTemperature() != null ? request.getTemperature() : 0.2,
            "max_tokens", request.getMaxTokens() > 0 ? request.getMaxTokens() : 2048
        );

        try {
            OpenAiResponse response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey.trim())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(OpenAiResponse.class);

            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                String text = response.getChoices().get(0).getMessage().getContent();
                String finishReason = response.getChoices().get(0).getFinishReason();
                int promptTokens = response.getUsage() != null ? response.getUsage().getPromptTokens() : 0;
                int completionTokens = response.getUsage() != null ? response.getUsage().getCompletionTokens() : 0;

                log.info("OpenAI response received: {} tokens (prompt={}, completion={})",
                         promptTokens + completionTokens, promptTokens, completionTokens);
                return new AiCompletionResponse(text, modelToUse, finishReason, promptTokens, completionTokens);
            }

            throw new RuntimeException("Empty response received from OpenAI API");

        } catch (Exception e) {
            log.error("Failed to execute live completion request against OpenAI API: {}", e.getMessage());
            throw new RuntimeException("OpenAI API Execution Failed: " + e.getMessage(), e);
        }
    }

    // ─── Response DTOs ─────────────────────────────────────────────────────────

    @Data
    private static class OpenAiResponse {
        private List<OpenAiChoice> choices;
        private OpenAiUsage usage;
    }

    @Data
    private static class OpenAiChoice {
        private OpenAiMessage message;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    private static class OpenAiMessage {
        private String content;
    }

    @Data
    private static class OpenAiUsage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        @JsonProperty("completion_tokens")
        private int completionTokens;
    }
}
