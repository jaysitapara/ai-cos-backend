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

@Slf4j
@Component
public class GeminiProviderImpl implements AiProvider {

    @Value("${ai.providers.gemini.api-key:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${ai.providers.gemini.model:gemini-1.5-flash}")
    private String defaultModel;

    private final RestClient restClient;

    public GeminiProviderImpl() {
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    @Override
    public String getProviderName() {
        return "Gemini";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public AiCompletionResponse generateCompletion(AiCompletionRequest request) {
        if (!isAvailable()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }

        String modelToUse = request.getModel() != null ? request.getModel() : defaultModel;
        log.info("Sending live HTTP request to Google Gemini API using model: {}", modelToUse);

        Map<String, Object> body = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", request.getPrompt())))
            )
        );

        try {
            GeminiResponse response = restClient.post()
                .uri(builder -> builder.path("/models/{model}:generateContent")
                        .queryParam("key", apiKey.trim())
                        .build(modelToUse))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(GeminiResponse.class);

            if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                GeminiCandidate candidate = response.getCandidates().get(0);
                String text = extractText(candidate);
                String finishReason = candidate.getFinishReason() != null ? candidate.getFinishReason() : "STOP";
                int promptTokens = response.getUsageMetadata() != null ? response.getUsageMetadata().getPromptTokenCount() : 0;
                int completionTokens = response.getUsageMetadata() != null ? response.getUsageMetadata().getCandidatesTokenCount() : 0;

                return new AiCompletionResponse(text, modelToUse, finishReason, promptTokens, completionTokens);
            }
        } catch (Exception e) {
            log.error("Failed to execute live completion request against Gemini API: {}", e.getMessage());
            throw new RuntimeException("Gemini API Execution Failed: " + e.getMessage(), e);
        }

        throw new RuntimeException("Empty response received from Gemini API");
    }

    private String extractText(GeminiCandidate candidate) {
        if (candidate.getContent() == null || candidate.getContent().getParts() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (GeminiPart part : candidate.getContent().getParts()) {
            if (part.getText() != null) {
                sb.append(part.getText());
            }
        }
        return sb.toString();
    }

    @Data
    private static class GeminiResponse {
        private List<GeminiCandidate> candidates;
        private GeminiUsageMetadata usageMetadata;
    }

    @Data
    private static class GeminiCandidate {
        private GeminiContent content;
        private String finishReason;
    }

    @Data
    private static class GeminiContent {
        private List<GeminiPart> parts;
    }

    @Data
    private static class GeminiPart {
        private String text;
    }

    @Data
    private static class GeminiUsageMetadata {
        @JsonProperty("promptTokenCount")
        private int promptTokenCount;
        @JsonProperty("candidatesTokenCount")
        private int candidatesTokenCount;
    }
}




    
    
    
    
