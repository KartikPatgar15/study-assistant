package com.studyassistant.service.ai;

/**
 * Thrown when an AI provider returns an error or is unreachable.
 *
 * <p>Caught by {@link com.studyassistant.exception.GlobalExceptionHandler}
 * and mapped to HTTP 503 Service Unavailable with a safe, user-facing message.
 * The original provider error is logged server-side but never exposed to the client.
 */
public class AiProviderException extends RuntimeException {

    public AiProviderException(String message) {
        super(message);
    }

    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
