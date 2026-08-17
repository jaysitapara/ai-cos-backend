package com.app.provider.ai;

import com.app.config.AppConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Single Active AI Provider Architecture — Isolation & Routing Test Suite")
class SingleProviderIsolationTest {

    private AiProvider geminiProvider;
    private AiProvider openAiProvider;
    private AppConfigService appConfigService;
    private AiProviderFactory aiProviderFactory;

    @BeforeEach
    void setUp() {
        geminiProvider = Mockito.mock(AiProvider.class);
        when(geminiProvider.getProviderName()).thenReturn("Gemini");

        openAiProvider = Mockito.mock(AiProvider.class);
        when(openAiProvider.getProviderName()).thenReturn("OpenAI");

        appConfigService = Mockito.mock(AppConfigService.class);
        aiProviderFactory = new AiProviderFactory(List.of(geminiProvider, openAiProvider), appConfigService);
    }

    @Test
    @DisplayName("When AI_MODE=gemini, Gemini provider is selected and OpenAI is NEVER called")
    void testGeminiActiveModeExcludesOpenAI() {
        when(appConfigService.getNormalizedAiMode()).thenReturn("gemini");
        AiCompletionResponse mockResponse = new AiCompletionResponse("Gemini output", "gemini-1.5-flash", "STOP", 10, 20);
        when(geminiProvider.generateCompletion(any())).thenReturn(mockResponse);

        AiProvider active = aiProviderFactory.getActiveProvider().orElseThrow();
        assertEquals("Gemini", active.getProviderName());

        AiCompletionRequest request = new AiCompletionRequest("Test Prompt", null, null, 0.2, 100, null);
        AiCompletionResponse response = aiProviderFactory.generateCompletion(request);

        assertNotNull(response);
        assertEquals("Gemini output", response.getContent());
        verify(geminiProvider, times(1)).generateCompletion(any());
        verify(openAiProvider, never()).generateCompletion(any());
    }

    @Test
    @DisplayName("When AI_MODE=openai, OpenAI provider is selected and Gemini is NEVER called")
    void testOpenAiActiveModeExcludesGemini() {
        when(appConfigService.getNormalizedAiMode()).thenReturn("openai");
        AiCompletionResponse mockResponse = new AiCompletionResponse("OpenAI output", "gpt-4o-mini", "STOP", 15, 25);
        when(openAiProvider.generateCompletion(any())).thenReturn(mockResponse);

        AiProvider active = aiProviderFactory.getActiveProvider().orElseThrow();
        assertEquals("OpenAI", active.getProviderName());

        AiCompletionRequest request = new AiCompletionRequest("Test Prompt", null, null, 0.2, 100, null);
        AiCompletionResponse response = aiProviderFactory.generateCompletion(request);

        assertNotNull(response);
        assertEquals("OpenAI output", response.getContent());
        verify(openAiProvider, times(1)).generateCompletion(any());
        verify(geminiProvider, never()).generateCompletion(any());
    }

    @Test
    @DisplayName("Active provider failure does NOT automatically trigger cross-provider fallback")
    void testNoAutomaticCrossProviderFallbackOnFailure() {
        when(appConfigService.getNormalizedAiMode()).thenReturn("gemini");
        when(geminiProvider.generateCompletion(any())).thenThrow(new RuntimeException("API Rate Limit Exceeded"));

        AiCompletionRequest request = new AiCompletionRequest("Test Prompt", null, null, 0.2, 100, null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> aiProviderFactory.generateCompletion(request));
        assertTrue(ex.getMessage().contains("Gemini"));

        // Verify OpenAI was NEVER called as a fallback
        verify(openAiProvider, never()).generateCompletion(any());
    }
}
