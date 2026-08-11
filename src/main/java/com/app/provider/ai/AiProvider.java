package com.app.provider.ai;

public interface AiProvider {
    String getProviderName();
    boolean isAvailable();
    AiCompletionResponse generateCompletion(AiCompletionRequest request);
}
