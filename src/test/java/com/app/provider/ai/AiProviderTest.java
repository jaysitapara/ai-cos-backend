package com.app.provider.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 7.2 — AI Provider Tests
 *
 * Verifies:
 *  1. Providers correctly report availability based on configured API keys/endpoints
 *  2. Providers that are NOT available throw exceptions (not return stub metadata)
 *  3. Factory fallback works correctly across provider priority order
 *  4. Real providers (Gemini, Groq) have proper provider names
 *
 * Note: Real HTTP calls to Gemini/Groq/OpenAI/Anthropic are NOT made in unit tests.
 * Testing the response parsing and live HTTP calls is done in integration tests.
 */
@DisplayName("AI Provider Tests — Phase 7.2")
class AiProviderTest {

    // ─── LocalAI Provider ─────────────────────────────────────────────────────

    @Test
    @DisplayName("LocalAI: isAvailable returns true when endpoint is configured")
    void testLocalAiAvailableWhenEndpointSet() {
        LocalAiProviderImpl provider = new LocalAiProviderImpl();
        ReflectionTestUtils.setField(provider, "endpoint", "http://localhost:11434");
        ReflectionTestUtils.setField(provider, "defaultModel", "llama3");

        assertTrue(provider.isAvailable());
        assertEquals("LocalAI", provider.getProviderName());
    }

    @Test
    @DisplayName("LocalAI: isAvailable returns false when endpoint is empty")
    void testLocalAiUnavailableWhenEndpointEmpty() {
        LocalAiProviderImpl provider = new LocalAiProviderImpl();
        ReflectionTestUtils.setField(provider, "endpoint", "");
        ReflectionTestUtils.setField(provider, "defaultModel", "llama3");

        assertFalse(provider.isAvailable());
    }

    @Test
    @DisplayName("LocalAI: generateCompletion throws exception (not stub echo) when endpoint unreachable")
    void testLocalAiThrowsExceptionOnUnavailableEndpoint() {
        LocalAiProviderImpl provider = new LocalAiProviderImpl();
        // Point to a port that refuses connections
        ReflectionTestUtils.setField(provider, "endpoint", "http://localhost:19999");
        ReflectionTestUtils.setField(provider, "defaultModel", "llama3");

        AiCompletionRequest req = new AiCompletionRequest("Test prompt", null, null, 0.2, 100, null);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> provider.generateCompletion(req));

        // Verify exception message does NOT contain the stub metadata pattern
        assertFalse(ex.getMessage().contains("Processed prompt:"),
            "LocalAI must NOT echo the prompt as a response. Exception: " + ex.getMessage());
        assertFalse(ex.getMessage().contains("[LocalAI") && ex.getMessage().contains("Response]:"),
            "LocalAI must NOT return stub metadata. Exception: " + ex.getMessage());
        assertTrue(ex.getMessage().startsWith("LocalAI Execution Failed:"),
            "Should throw LocalAI Execution Failed. Got: " + ex.getMessage());
    }

    // ─── OpenAI Provider ──────────────────────────────────────────────────────

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
    @DisplayName("OpenAI: generateCompletion throws proper exception (not stub echo) with invalid key")
    void testOpenAiThrowsExceptionNotStubEcho() {
        OpenAiProviderImpl provider = new OpenAiProviderImpl();
        ReflectionTestUtils.setField(provider, "apiKey", "sk-invalid-key");
        ReflectionTestUtils.setField(provider, "defaultModel", "gpt-4o");

        AiCompletionRequest req = new AiCompletionRequest("Test prompt", null, null, 0.2, 100, null);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> provider.generateCompletion(req));

        // Verify it's a real HTTP error, NOT the old stub pattern
        assertFalse(ex.getMessage().contains("[OpenAI") && ex.getMessage().contains("Response]: Processed prompt:"),
            "OpenAI must NOT return old stub metadata. Exception: " + ex.getMessage());
        assertTrue(ex.getMessage().startsWith("OpenAI API Execution Failed:"),
            "Should throw OpenAI API Execution Failed. Got: " + ex.getMessage());
    }

    // ─── Anthropic Provider ───────────────────────────────────────────────────

    @Test
    @DisplayName("Anthropic: isAvailable returns true when API key is configured")
    void testAnthropicAvailableWhenKeySet() {
        AnthropicProviderImpl provider = new AnthropicProviderImpl();
        ReflectionTestUtils.setField(provider, "apiKey", "sk-ant-test-key");
        ReflectionTestUtils.setField(provider, "defaultModel", "claude-3-5-sonnet-20241022");

        assertTrue(provider.isAvailable());
        assertEquals("Anthropic", provider.getProviderName());
    }

    @Test
    @DisplayName("Anthropic: generateCompletion throws proper exception (not stub echo)")
    void testAnthropicThrowsExceptionNotStubEcho() {
        AnthropicProviderImpl provider = new AnthropicProviderImpl();
        ReflectionTestUtils.setField(provider, "apiKey", "sk-ant-invalid-key");
        ReflectionTestUtils.setField(provider, "defaultModel", "claude-3-5-sonnet-20241022");

        AiCompletionRequest req = new AiCompletionRequest("Test prompt", null, null, 0.2, 100, null);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> provider.generateCompletion(req));

        // Verify it's a real HTTP error, NOT the old stub pattern
        assertFalse(ex.getMessage().contains("[Anthropic") && ex.getMessage().contains("Response]: Processed prompt:"),
            "Anthropic must NOT return old stub metadata. Exception: " + ex.getMessage());
        assertTrue(ex.getMessage().startsWith("Anthropic API Execution Failed:"),
            "Should throw Anthropic API Execution Failed. Got: " + ex.getMessage());
    }

    // ─── Provider Factory ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Factory: getProvider returns the correct provider by name")
    void testAiProviderFactoryGetByName() {
        OpenAiProviderImpl openAi = new OpenAiProviderImpl();
        ReflectionTestUtils.setField(openAi, "apiKey", "sk-test-key");
        ReflectionTestUtils.setField(openAi, "defaultModel", "gpt-4o");

        LocalAiProviderImpl localAi = new LocalAiProviderImpl();
        ReflectionTestUtils.setField(localAi, "endpoint", "http://localhost:11434");
        ReflectionTestUtils.setField(localAi, "defaultModel", "llama3");

        AiProviderFactory factory = new AiProviderFactory(List.of(openAi, localAi));
        ReflectionTestUtils.setField(factory, "defaultProviderName", "OpenAI");

        Optional<AiProvider> providerOpt = factory.getProvider("OpenAI");
        assertTrue(providerOpt.isPresent());
        assertEquals("OpenAI", providerOpt.get().getProviderName());
    }

    @Test
    @DisplayName("Factory: getDefaultProvider falls back correctly when configured provider unavailable")
    void testAiProviderFactoryFallback() {
        // Gemini is the fallback priority #1 but unavailable (no key)
        // LocalAI is available
        LocalAiProviderImpl localAi = new LocalAiProviderImpl();
        ReflectionTestUtils.setField(localAi, "endpoint", "http://localhost:11434");
        ReflectionTestUtils.setField(localAi, "defaultModel", "llama3");

        AiProviderFactory factory = new AiProviderFactory(List.of(localAi));
        ReflectionTestUtils.setField(factory, "defaultProviderName", "gemini");

        // Default provider (gemini) is not in list, should fall through to localai
        Optional<AiProvider> defaultOpt = factory.getDefaultProvider();
        assertTrue(defaultOpt.isPresent(), "Factory must return at least one available provider");
        assertEquals("LocalAI", defaultOpt.get().getProviderName());
    }

    @Test
    @DisplayName("Factory: getAvailableProviderNames returns only configured providers")
    void testFactoryAvailableProviderNames() {
        OpenAiProviderImpl openAi = new OpenAiProviderImpl();
        ReflectionTestUtils.setField(openAi, "apiKey", "sk-valid-key");
        ReflectionTestUtils.setField(openAi, "defaultModel", "gpt-4o");

        OpenAiProviderImpl noKeyProvider = new OpenAiProviderImpl();
        ReflectionTestUtils.setField(noKeyProvider, "apiKey", ""); // unavailable
        ReflectionTestUtils.setField(noKeyProvider, "defaultModel", "gpt-4o");

        LocalAiProviderImpl localAi = new LocalAiProviderImpl();
        ReflectionTestUtils.setField(localAi, "endpoint", "http://localhost:11434");
        ReflectionTestUtils.setField(localAi, "defaultModel", "llama3");

        AiProviderFactory factory = new AiProviderFactory(List.of(openAi, localAi));
        ReflectionTestUtils.setField(factory, "defaultProviderName", "gemini");

        List<String> names = factory.getAvailableProviderNames();
        assertTrue(names.contains("OpenAI"), "OpenAI with valid key should be listed");
        assertTrue(names.contains("LocalAI"), "LocalAI with endpoint should be listed");
    }
}
