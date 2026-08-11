package com.app.service;

import com.app.entity.UserEntity;
import com.app.provider.ai.AiCompletionRequest;
import com.app.provider.ai.AiCompletionResponse;
import com.app.provider.ai.AiProvider;
import com.app.provider.ai.AiProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final AiProviderFactory aiProviderFactory;
    private final AiUsageTrackingService usageTrackingService;

    public AiCompletionResponse generateCompletion(AiCompletionRequest request) {
        return generateCompletion(request, null, null, null, "COMPLETION", null);
    }

    public AiCompletionResponse generateCompletion(AiCompletionRequest request,
                                                    UserEntity user,
                                                    Long projectId,
                                                    String jobId,
                                                    String requestType,
                                                    String agentRole) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        AiProvider activeProvider = aiProviderFactory.getActiveProvider()
                .orElseThrow(() -> new IllegalStateException("No active AI provider configured"));
        String providerName = activeProvider.getProviderName();

        try {
            log.info("AIService invoking active provider [{}] for requestType={} agentRole={}", providerName, requestType, agentRole);
            AiCompletionResponse response = aiProviderFactory.generateCompletion(request);
            long latency = System.currentTimeMillis() - startTime;

            usageTrackingService.logAiUsage(
                    user,
                    projectId,
                    jobId,
                    requestId,
                    providerName,
                    response.getModelName(),
                    requestType,
                    agentRole,
                    (int) response.getPromptTokens(),
                    (int) response.getCompletionTokens(),
                    latency,
                    "SUCCESS",
                    null
            );

            return response;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("AIService call failed via provider [{}]: {}", providerName, e.getMessage());

            usageTrackingService.logAiUsage(
                    user,
                    projectId,
                    jobId,
                    requestId,
                    providerName,
                    request.getModel() != null ? request.getModel() : "default",
                    requestType,
                    agentRole,
                    0,
                    0,
                    latency,
                    "FAILED",
                    e.getMessage()
            );

            throw e;
        }
    }

    public String analyzePrompt(String systemInstruction, String promptText, UserEntity user, Long projectId, String agentRole) {
        AiCompletionRequest request = new AiCompletionRequest(promptText, systemInstruction, null, 0.2, 2048, Map.of());
        AiCompletionResponse response = generateCompletion(request, user, projectId, null, "ANALYSIS", agentRole);
        return response != null ? response.getContent() : "";
    }
}
