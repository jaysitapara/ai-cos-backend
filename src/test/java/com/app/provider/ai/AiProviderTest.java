package com.app.provider.ai;

import com.app.config.AppConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AI Provider Tests — Verification")
class AiProviderTest {

    @Test
    @DisplayName("OpenAI: isAvailable returns true when API key is configured")
    void testOpenAiAvailableWhenKeySet() {
        OpenAiProviderImpl provider = new OpenAiProviderImpl();
        ReflectionTestUtils.setField(provider, "apiKey", "sk-test-key-1234");
        ReflectionTestUtils.setField(provider, "defaultModel", "gpt-4o");

        assertTrue(provider.isAvailable());
        assertEquals("OpenAI", provider.getProviderName());
    }

    @Test
    @DisplayName("OpenAI: isAvailable returns false when API key is blank")
    void testOpenAiUnavailableWhenKeyBlank() {
        OpenAiProviderImpl provider = new OpenAiProviderImpl();
        ReflectionTestUtils.setField(provider, "apiKey", "  ");
        ReflectionTestUtils.setField(provider, "defaultModel", "gpt-4o");

        assertFalse(provider.isAvailable());
    }

    @Test
    @DisplayName("OpenAI: generateCompletion throws proper exception with invalid key")
    void testOpenAiThrowsExceptionNotStubEcho() {
        OpenAiProviderImpl provider = new OpenAiProviderImpl();
        ReflectionTestUtils.setField(provider, "apiKey", "sk-invalid-key");
        ReflectionTestUtils.setField(provider, "defaultModel", "gpt-4o");

        AiCompletionRequest req = new AiCompletionRequest("Test prompt", null, null, 0.2, 100, null);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> provider.generateCompletion(req));

        assertTrue(ex.getMessage().startsWith("OpenAI API Execution Failed:"),
            "Should throw OpenAI API Execution Failed. Got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Factory: getActiveProvider selects Gemini when AI_MODE=gemini")
    void testFactoryActiveProviderGemini() {
        GeminiProviderImpl gemini = new GeminiProviderImpl();
        ReflectionTestUtils.setField(gemini, "apiKey", "test-gemini-key");

        OpenAiProviderImpl openAi = new OpenAiProviderImpl();
        ReflectionTestUtils.setField(openAi, "apiKey", "test-openai-key");

        AppConfigService configService = Mockito.mock(AppConfigService.class);
        Mockito.when(configService.getNormalizedAiMode()).thenReturn("gemini");

        AiProviderFactory factory = new AiProviderFactory(List.of(gemini, openAi), configService);

        Optional<AiProvider> activeOpt = factory.getActiveProvider();
        assertTrue(activeOpt.isPresent());
        assertEquals("Gemini", activeOpt.get().getProviderName());
    }

    @Test
    @DisplayName("Factory: getActiveProvider selects OpenAI when AI_MODE=openai")
    void testFactoryActiveProviderOpenAi() {
        GeminiProviderImpl gemini = new GeminiProviderImpl();
        ReflectionTestUtils.setField(gemini, "apiKey", "test-gemini-key");

        OpenAiProviderImpl openAi = new OpenAiProviderImpl();
        ReflectionTestUtils.setField(openAi, "apiKey", "test-openai-key");

        AppConfigService configService = Mockito.mock(AppConfigService.class);
        Mockito.when(configService.getNormalizedAiMode()).thenReturn("openai");

        AiProviderFactory factory = new AiProviderFactory(List.of(gemini, openAi), configService);

        Optional<AiProvider> activeOpt = factory.getActiveProvider();
        assertTrue(activeOpt.isPresent());
        assertEquals("OpenAI", activeOpt.get().getProviderName());
    }

    @Test
    @DisplayName("Factory: throws IllegalStateException when AI_MODE is invalid")
    void testFactoryFailsFastOnInvalidAiMode() {
        GeminiProviderImpl gemini = new GeminiProviderImpl();
        OpenAiProviderImpl openAi = new OpenAiProviderImpl();

        AppConfigService configService = Mockito.mock(AppConfigService.class);
        Mockito.when(configService.getNormalizedAiMode()).thenReturn("invalid_provider");

        AiProviderFactory factory = new AiProviderFactory(List.of(gemini, openAi), configService);

        assertThrows(IllegalStateException.class, factory::getActiveProvider);
    }

    @Test
    @DisplayName("Factory: generateCompletion throws exception on active provider failure without fallback")
    void testNoAutomaticFallbackOnProviderFailure() {
        AiProvider failingGemini = Mockito.mock(AiProvider.class);
        Mockito.when(failingGemini.getProviderName()).thenReturn("Gemini");
        Mockito.when(failingGemini.generateCompletion(Mockito.any())).thenThrow(new RuntimeException("Gemini API down"));

        AiProvider openAi = Mockito.mock(AiProvider.class);
        Mockito.when(openAi.getProviderName()).thenReturn("OpenAI");

        AppConfigService configService = Mockito.mock(AppConfigService.class);
        Mockito.when(configService.getNormalizedAiMode()).thenReturn("gemini");

        AiProviderFactory factory = new AiProviderFactory(List.of(failingGemini, openAi), configService);

        AiCompletionRequest req = new AiCompletionRequest("Prompt", null, null, 0.2, 100, null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> factory.generateCompletion(req));

        assertTrue(ex.getMessage().contains("Gemini API down"));
        Mockito.verify(openAi, Mockito.never()).generateCompletion(Mockito.any());
    }
}
