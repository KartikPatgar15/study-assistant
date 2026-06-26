package com.studyassistant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource does not exist.
 *
 * <p>Handled by {@link GlobalExceptionHandler} which maps it to HTTP 404.
 * Future modules throw this from service layers rather than constructing
 * ResponseEntity objects by hand in controllers.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceType, Object id) {
        super(String.format("%s with id '%s' not found", resourceType, id));
    }
}
