package com.studyassistant.service.ai;

/**
 * Provider-independent contract for AI text generation.
 *
 * <p>Any AI backend (Gemini, Groq, OpenRouter, etc.) implements this interface.
 * {@link com.studyassistant.service.ai.ChatService} depends only on this
 * abstraction, so swapping providers requires no changes outside the
 * {@code service.ai} package.
 *
 * <p>M04A ships with {@link GeminiProvider}. Future modules add alternatives
 * and select the active provider via {@code app.ai.provider} in
 * {@code application.properties}.
 */
public interface AiProvider {

    /**
     * Send a complete prompt to the AI provider and return the response text.
     *
     * @param prompt the fully-assembled prompt (system context + study material + question)
     * @return the provider's response text; never null
     * @throws AiProviderException if the provider returns an error or is unreachable
     */
    String ask(String prompt);

    /**
     * Returns the provider name for logging and response metadata.
     * Example: {@code "gemini"}
     */
    String providerName();

    /**
     * Returns the model identifier for response metadata.
     * Example: {@code "gemini-2.5-flash"}
     */
    String modelName();
}
