package com.studyassistant.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error response body returned by {@link GlobalExceptionHandler}.
 *
 * <p>All API errors share this shape, so frontend code only needs one error
 * parsing path. Fields marked {@code @JsonInclude(NON_NULL)} are omitted when
 * absent so simple errors don't include empty arrays.
 *
 * <pre>
 * {
 *   "timestamp": "2024-09-01T12:00:00Z",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Resource not found",
 *   "path": "/api/documents/999",
 *   "fieldErrors": [ ... ]   // only present for validation failures
 * }
 * </pre>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;

    /** Present only when request validation fails (HTTP 400). */
    private final List<FieldError> fieldErrors;

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String message;
    }
}
