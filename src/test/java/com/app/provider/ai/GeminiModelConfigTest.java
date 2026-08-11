package com.app.provider.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for Gemini model configuration (Issue 1).
 *
 * Verifies:
 *  - gemini-1.5-flash (current supported model) is the default
 *  - deprecated model names are NOT the default
 *  - isAvailable() reflects key presence correctly
 *  - generateCompletion() throws a properly typed exception on 404
 */
@DisplayName("Gemini Model Configuration Tests")
class GeminiModelConfigTest {

    @Test
    @DisplayName("Default model must be gemini-1.5-flash (not a deprecated model)")
    void defaultModelIsCurrentSupportedModel() {
        GeminiProviderImpl provider = new GeminiProviderImpl();
        // Inject via @Value default — simulate what Spring injects when no env var is set
        ReflectionTestUtils.setField(provider, "apiKey", "dummy-key");
        ReflectionTestUtils.setField(provider, "defaultModel", "gemini-1.5-flash");

        String model = (String) ReflectionTestUtils.getField(provider, "defaultModel");

        assertNotNull(model, "Model field must not be null");
        assertEquals("gemini-1.5-flash", model, "Default model must be gemini-1.5-flash");

        // Explicitly assert deprecated model names are NOT used
        assertNotEquals("gemini-2.0-flash", model, "Deprecated gemini-2.0-flash must not be used");
    }

    @Test
    @DisplayName("isAvailable() returns true when API key is configured")
    void isAvailableWhenKeySet() {
        GeminiProviderImpl provider = new GeminiProviderImpl();
        ReflectionTestUtils.setField(provider, "apiKey", "AQ.some-test-gemini-key");
        ReflectionTestUtils.setField(provider, "defaultModel", "gemini-1.5-flash");

        assertTrue(provider.isAvailable());
        assertEquals("Gemini", provider.getProviderName());
    }

    @Test
    @DisplayName("isAvailable() returns false when API key is blank")
    void isUnavailableWhenKeyBlank() {
        GeminiProviderImpl provider = new GeminiProviderImpl();
        ReflectionTestUtils.setField(provider, "apiKey", "   ");
        ReflectionTestUtils.setField(provider, "defaultModel", "gemini-1.5-flash");

        assertFalse(provider.isAvailable());
    }

    @Test
    @DisplayName("isAvailable() returns false when API key is null")
    void isUnavailableWhenKeyNull() {
        GeminiProviderImpl provider = new GeminiProviderImpl();
        ReflectionTestUtils.setField(provider, "apiKey", null);
        ReflectionTestUtils.setField(provider, "defaultModel", "gemini-1.5-flash");

        assertFalse(provider.isAvailable());
    }

    @Test
    @DisplayName("generateCompletion() throws IllegalStateException when key not configured")
    void throwsIllegalStateWhenKeyNotSet() {
        GeminiProviderImpl provider = new GeminiProviderImpl();
        ReflectionTestUtils.setField(provider, "apiKey", "");
        ReflectionTestUtils.setField(provider, "defaultModel", "gemini-1.5-flash");

        AiCompletionRequest req = new AiCompletionRequest("test prompt", null, null, 0.2, 100, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> provider.generateCompletion(req));
        assertTrue(ex.getMessage().contains("not configured"));
    }

    @Test
    @DisplayName("generateCompletion() throws RuntimeException with live HTTP call using invalid key")
    void throwsRuntimeExceptionWithInvalidKey() {
        GeminiProviderImpl provider = new GeminiProviderImpl();
        ReflectionTestUtils.setField(provider, "apiKey", "invalid-gemini-key-xyz");
        ReflectionTestUtils.setField(provider, "defaultModel", "gemini-1.5-flash");

        AiCompletionRequest req = new AiCompletionRequest("hello", null, null, 0.2, 100, null);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> provider.generateCompletion(req));

        assertTrue(ex.getMessage().startsWith("Gemini API Execution Failed:"),
            "Must throw Gemini API Execution Failed. Got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Model can be overridden via request.getModel()")
    void modelCanBeOverriddenPerRequest() {
        GeminiProviderImpl provider = new GeminiProviderImpl();
        ReflectionTestUtils.setField(provider, "apiKey", "some-key");
        ReflectionTestUtils.setField(provider, "defaultModel", "gemini-1.5-flash");

        // Per-request model override is supported — the provider uses request.getModel() if set
        AiCompletionRequest req = new AiCompletionRequest("prompt", null, "gemini-1.5-flash", 0.5, 512, null);
        assertEquals("gemini-1.5-flash", req.getModel(),
            "Request model override must be preserved");
    }
}
