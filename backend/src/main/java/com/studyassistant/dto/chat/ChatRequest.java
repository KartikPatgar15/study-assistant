package com.studyassistant.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/chat}.
 *
 * <p>Both fields are validated before reaching the service layer.
 * Validation errors are handled by {@code GlobalExceptionHandler} → HTTP 400.
 */
public class ChatRequest {

    @NotBlank(message = "uploadId must not be blank")
    private String uploadId;

    @NotBlank(message = "question must not be blank")
    @Size(max = 2000, message = "question must not exceed 2000 characters")
    private String question;

    // ── Getters & setters (required for Jackson deserialisation) ──────────────

    public String getUploadId()             { return uploadId; }
    public void   setUploadId(String v)     { uploadId = v; }

    public String getQuestion()             { return question; }
    public void   setQuestion(String v)     { question = v; }
}
