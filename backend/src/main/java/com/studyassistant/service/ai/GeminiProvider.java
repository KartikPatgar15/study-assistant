package com.studyassistant.service.ai;

import com.studyassistant.config.AiConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Google Gemini implementation of {@link AiProvider}.
 *
 * <p>Uses the Gemini {@code generateContent} REST endpoint via Spring WebClient.
 * The API key is appended as a query parameter (Google's required auth mechanism
 * for this endpoint) and is never written to logs.
 *
 * <p>Error handling:
 * <ul>
 *   <li>4xx (bad request, invalid key) → {@link AiProviderException} with a safe message.</li>
 *   <li>5xx (Gemini service error)     → {@link AiProviderException} with a safe message.</li>
 *   <li>Network / timeout errors       → {@link AiProviderException} wrapping the cause.</li>
 *   <li>Unexpected response shape      → {@link AiProviderException}.</li>
 * </ul>
 *
 * <p>The raw provider error detail is logged at ERROR level server-side;
 * only the safe wrapper message is propagated to the client.
 */
@Slf4j
@Service
public class GeminiProvider implements AiProvider {

    private final AiConfiguration config;
    private final WebClient       webClient;

    public GeminiProvider(AiConfiguration config, WebClient.Builder webClientBuilder) {
        this.config    = config;
        this.webClient = webClientBuilder
                .baseUrl(config.getGemini().getBaseUrl())
                .build();
    }

    // ── AiProvider ────────────────────────────────────────────────────────────

    @Override
    public String ask(String prompt) {
        String model  = config.getGemini().getModel();
        String apiKey = config.getGemini().getApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            throw new AiProviderException(
                    "Gemini API key is not configured. Set app.ai.gemini.api-key in " +
                    "application-local.properties or the APP_AI_GEMINI_API-KEY environment variable.");
        }

        // Log intent without exposing the key
        log.info("Sending prompt to Gemini model='{}', prompt-length={} chars", model, prompt.length());

        // Build the request body per the Gemini generateContent API spec
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        try {
            // Synchronous call – streaming is deferred to a future module
            GeminiResponse response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .block();

            if (response == null
                    || response.candidates() == null
                    || response.candidates().isEmpty()) {
                log.error("Gemini returned an empty response for model='{}'", model);
                throw new AiProviderException("The AI provider returned an empty response. Please try again.");
            }

            String text = extractText(response);
            log.info("Gemini response received – response-length={} chars", text.length());
            return text;

        } catch (WebClientResponseException e) {
            log.error("Gemini API error – status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiProviderException(
                    "The AI provider returned an error (" + e.getStatusCode().value() +
                    "). Please check your API key and try again.", e);
        } catch (AiProviderException e) {
            throw e; // already wrapped
        } catch (Exception e) {
            log.error("Unexpected error communicating with Gemini", e);
            throw new AiProviderException("Could not reach the AI provider. Please try again later.", e);
        }
    }

    @Override
    public String providerName() { return "gemini"; }

    @Override
    public String modelName() { return config.getGemini().getModel(); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractText(GeminiResponse response) {
        try {
            return response.candidates().get(0)
                    .content().parts().get(0)
                    .text();
        } catch (Exception e) {
            log.error("Could not extract text from Gemini response structure", e);
            throw new AiProviderException("The AI provider returned an unreadable response.");
        }
    }

    // ── Response shape records (maps Gemini JSON structure) ───────────────────

    static record GeminiResponse(List<Candidate> candidates) {}
    static record Candidate(Content content) {}
    static record Content(List<Part> parts) {}
    static record Part(String text) {}
}
