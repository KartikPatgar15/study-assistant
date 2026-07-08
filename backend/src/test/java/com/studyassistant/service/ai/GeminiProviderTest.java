package com.studyassistant.service.ai;

import com.studyassistant.config.AiConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GeminiProvider}.
 * All HTTP interactions are mocked – no real API calls are made.
 */
@ExtendWith(MockitoExtension.class)
class GeminiProviderTest {

    @Mock private WebClient.Builder       webClientBuilder;
    @Mock private WebClient               webClient;
    @Mock private WebClient.RequestBodyUriSpec  requestBodyUriSpec;
    @Mock private WebClient.RequestBodySpec     requestBodySpec;
    @Mock private WebClient.RequestHeadersSpec  requestHeadersSpec;
    @Mock private WebClient.ResponseSpec        responseSpec;

    private AiConfiguration config;
    private GeminiProvider  provider;

    @BeforeEach
    void setUp() {
        config = new AiConfiguration();
        config.getGemini().setApiKey("test-api-key");
        config.getGemini().setModel("gemini-2.5-flash");
        config.getGemini().setBaseUrl("https://generativelanguage.googleapis.com");

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        provider = new GeminiProvider(config, webClientBuilder);
    }

    @Test
    void providerName_returnsGemini() {
        assertThat(provider.providerName()).isEqualTo("gemini");
    }

    @Test
    void modelName_returnsConfiguredModel() {
        assertThat(provider.modelName()).isEqualTo("gemini-2.5-flash");
    }

    @Test
    void ask_missingApiKey_throwsAiProviderException() {
        config.getGemini().setApiKey("");
        assertThatThrownBy(() -> provider.ask("test prompt"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("API key is not configured");
    }

    @Test
    void ask_blankApiKey_throwsAiProviderException() {
        config.getGemini().setApiKey("   ");
        assertThatThrownBy(() -> provider.ask("test prompt"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("API key is not configured");
    }

    @SuppressWarnings("unchecked")
    @Test
    void ask_successfulResponse_returnsAnswerText() {
        setupMockChain("This is the AI answer.");

        String result = provider.ask("What is an operating system?");

        assertThat(result).isEqualTo("This is the AI answer.");
    }

    @SuppressWarnings("unchecked")
    @Test
    void ask_webClientError_throwsAiProviderException() {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(Class.class)))
                .thenReturn(Mono.error(WebClientResponseException.create(429, "Too Many Requests", null, null, null)));

        assertThatThrownBy(() -> provider.ask("test"))
                .isInstanceOf(AiProviderException.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void setupMockChain(String answerText) {
        // Build a fake GeminiResponse via reflection-safe inner records
        // by mocking the WebClient chain to return a valid response body
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        // Return a valid JSON-deserialised response via a real object
        var part      = new GeminiProvider.Part(answerText);
        var content   = new GeminiProvider.Content(java.util.List.of(part));
        var candidate = new GeminiProvider.Candidate(content);
        var response  = new GeminiProvider.GeminiResponse(java.util.List.of(candidate));

        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(response));
    }
}
