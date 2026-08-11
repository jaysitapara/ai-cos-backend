package com.app.service;

import com.app.entity.AiUsageLogEntity;
import com.app.entity.UserEntity;
import com.app.repository.AiUsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiUsageTrackingService {

    private final AiUsageLogRepository usageLogRepository;
    private final TokenCostCalculator costCalculator;

    public void logAiUsage(UserEntity user,
                           Long projectId,
                           String jobId,
                           String requestId,
                           String provider,
                           String model,
                           String requestType,
                           String agentRole,
                           int promptTokens,
                           int completionTokens,
                           long latencyMs,
                           String status,
                           String errorMessage) {
        try {
            int totalTokens = promptTokens + completionTokens;
            double cost = costCalculator.calculateCost(model, promptTokens, completionTokens);

            AiUsageLogEntity usageLog = AiUsageLogEntity.builder()
                    .publicId(UUID.randomUUID())
                    .user(user)
                    .projectId(projectId)
                    .jobId(jobId)
                    .requestId(requestId)
                    .provider(provider)
                    .model(model)
                    .requestType(requestType)
                    .agentRole(agentRole)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(totalTokens)
                    .latencyMs(latencyMs)
                    .status(status)
                    .errorMessage(errorMessage)
                    .estimatedCost(BigDecimal.valueOf(cost))
                    .createdAt(OffsetDateTime.now())
                    .build();

            usageLogRepository.save(usageLog);
            log.debug("Logged AI Telemetry: Provider={}, Model={}, Tokens={}, Latency={}ms, EstCost=${}",
                    provider, model, totalTokens, latencyMs, cost);
        } catch (Exception e) {
            log.error("Failed to log AI usage telemetry: {}", e.getMessage(), e);
        }
    }
}
