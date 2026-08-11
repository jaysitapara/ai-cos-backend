package com.app.provider.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private final List<String> fallbackPriorityOrder = List.of("gemini", "groq", "openai", "anthropic", "localai");

    @Value("${ai.provider.default:Gemini}")
    private String defaultProviderName;

    @Autowired
    public AiProviderFactory(List<AiProvider> providerList) {
        this.providers = providerList.stream()
            .collect(Collectors.toMap(
                p -> p.getProviderName().toLowerCase(),
                Function.identity(),
                (existing, replacement) -> existing
            ));
    }

    public Optional<AiProvider> getProvider(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getDefaultProvider();
        }
        return Optional.ofNullable(providers.get(name.toLowerCase()));
    }

    public Optional<AiProvider> getDefaultProvider() {
        AiProvider configured = providers.get(defaultProviderName.toLowerCase());
        if (configured != null && configured.isAvailable()) {
            return Optional.of(configured);
        }
        // Fallback priority: Gemini -> Groq -> OpenAI -> Anthropic -> LocalAI
        for (String priorityName : fallbackPriorityOrder) {
            AiProvider p = providers.get(priorityName);
            if (p != null && p.isAvailable()) {
                log.info("Default provider not available; falling back to priority provider: {}", p.getProviderName());
                return Optional.of(p);
            }
        }
        return providers.values().stream().filter(AiProvider::isAvailable).findFirst();
    }

    /**
     * Executes completion request with automatic provider failover across fallback order.
     */
    public AiCompletionResponse generateCompletionWithFallback(AiCompletionRequest request) {
        // Build candidate provider list starting with requested or default
        List<AiProvider> availableCandidates = fallbackPriorityOrder.stream()
            .map(providers::get)
            .filter(p -> p != null && p.isAvailable())
            .collect(Collectors.toList());

        if (availableCandidates.isEmpty()) {
            // Fallback to any registered provider if no keys configured
            availableCandidates = providers.values().stream().collect(Collectors.toList());
        }

        Exception lastException = null;
        for (AiProvider provider : availableCandidates) {
            try {
                log.info("Attempting LLM completion using provider: {}", provider.getProviderName());
                return provider.generateCompletion(request);
            } catch (Exception e) {
                log.warn("Provider {} failed execution: {}. Trying failover provider...", provider.getProviderName(), e.getMessage());
                lastException = e;
            }
        }

        throw new RuntimeException("All AI Providers failed to generate completion. Last error: " +
            (lastException != null ? lastException.getMessage() : "Unknown"), lastException);
    }

    public List<String> getAvailableProviderNames() {
        return providers.values().stream()
            .filter(AiProvider::isAvailable)
            .map(AiProvider::getProviderName)
            .collect(Collectors.toList());
    }
}
