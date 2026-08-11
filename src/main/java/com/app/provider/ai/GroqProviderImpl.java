package com.app.provider.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GroqProviderImpl implements AiProvider {

    @Value("${groq.api-key:${GROQ_API_KEY:}}")
    private String apiKey;

    @Value("${ai.providers.groq.model:llama-3.3-70b-versatile}")
    private String defaultModel;

    private final RestClient restClient;

    public GroqProviderImpl() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .build();
    }

    @Override
    public String getProviderName() {
        return "Groq";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public AiCompletionResponse generateCompletion(AiCompletionRequest request) {
        if (!isAvailable()) {
            throw new IllegalStateException("Groq API key is not configured");
        }

        String modelToUse = request.getModel() != null ? request.getModel() : defaultModel;
        log.info("Sending live HTTP request to Groq API using model: {}", modelToUse);

        Map<String, Object> body = Map.of(
            "model", modelToUse,
            "messages", List.of(Map.of("role", "user", "content", request.getPrompt())),
            "temperature", 0.2
        );

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                GroqResponse response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GroqResponse.class);

                if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                    String text = response.getChoices().get(0).getMessage().getContent();
                    String finishReason = response.getChoices().get(0).getFinishReason();
                    int promptTokens = response.getUsage() != null ? response.getUsage().getPromptTokens() : 0;
                    int completionTokens = response.getUsage() != null ? response.getUsage().getCompletionTokens() : 0;

                    return new AiCompletionResponse(text, modelToUse, finishReason, promptTokens, completionTokens);
                }
            } catch (Exception e) {
                if (e.getMessage() != null && (e.getMessage().contains("429") || e.getMessage().contains("rate_limit"))) {
                    log.warn("Groq API rate limit hit (attempt {}/{}). Waiting 3 seconds before retrying...", attempt, maxRetries);
                    try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                    if (attempt < maxRetries) continue;
                }
                log.error("Failed to execute live completion request against Groq API on attempt {}: {}", attempt, e.getMessage());
                throw new RuntimeException("Groq API Execution Failed: " + e.getMessage(), e);
            }
        }

        throw new RuntimeException("Empty response received from Groq API");
    }

    @Data
    private static class GroqResponse {
        private List<GroqChoice> choices;
        private GroqUsage usage;
    }

    @Data
    private static class GroqChoice {
        private GroqMessage message;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    private static class GroqMessage {
        private String content;
    }

    @Data
    private static class GroqUsage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        @JsonProperty("completion_tokens")
        private int completionTokens;
    }
}
