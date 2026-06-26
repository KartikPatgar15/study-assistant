package com.studyassistant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the caller sends a semantically invalid request (HTTP 400).
 *
 * <p>Distinct from bean validation failures, which are handled automatically
 * by Spring via {@code @Valid} and reported through the same {@link ApiError}
 * structure by {@link GlobalExceptionHandler}.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
