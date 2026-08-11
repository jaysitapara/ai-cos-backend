package com.app.provider.ai;

import com.app.config.AppConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiProviderFactory {

    private final Map<String, AiProvider> providers;
    private final AppConfigService appConfigService;

    @Autowired
    public AiProviderFactory(List<AiProvider> providerList, AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
        this.providers = providerList.stream()
            .collect(Collectors.toMap(
                p -> p.getProviderName().toLowerCase(),
                Function.identity(),
                (existing, replacement) -> existing
            ));
    }

    public Optional<AiProvider> getProvider(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getActiveProvider();
        }
        String normalized = name.trim().toLowerCase();
        if (!"gemini".equals(normalized) && !"openai".equals(normalized)) {
            throw new IllegalArgumentException("Unsupported AI provider requested: '" + name + "'. Supported providers: gemini, openai.");
        }
        return Optional.ofNullable(providers.get(normalized));
    }

    public Optional<AiProvider> getActiveProvider() {
        String activeMode = appConfigService.getNormalizedAiMode();
        if (!"gemini".equals(activeMode) && !"openai".equals(activeMode)) {
            throw new IllegalStateException("Unsupported AI_MODE configured: '" + activeMode + "'. Supported modes: gemini, openai.");
        }

        AiProvider provider = providers.get(activeMode);
        if (provider == null) {
            throw new IllegalStateException("No provider implementation registered for active AI_MODE: '" + activeMode + "'");
        }
        return Optional.of(provider);
    }

    /**
     * Executes completion using strictly the backend-configured active AI provider (AI_MODE).
     * IMPORTANT: No automatic failover or fallback to another provider is permitted per architecture spec.
     */
    public AiCompletionResponse generateCompletion(AiCompletionRequest request) {
        AiProvider provider = getActiveProvider()
            .orElseThrow(() -> new IllegalStateException("Active AI provider is not available"));

        log.info("Executing LLM completion strictly using active AI_MODE provider: {}", provider.getProviderName());
        try {
            return provider.generateCompletion(request);
        } catch (Exception e) {
            log.error("Active AI provider [{}] failed execution: {}. Automatic fallback is DISABLED.", provider.getProviderName(), e.getMessage());
            throw new RuntimeException("AI Provider [" + provider.getProviderName() + "] Execution Failed: " + e.getMessage(), e);
        }
    }

    public List<String> getAvailableProviderNames() {
        return List.of("gemini", "openai");
    }
}

