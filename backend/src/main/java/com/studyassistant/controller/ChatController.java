package com.studyassistant.controller;

import com.studyassistant.dto.chat.ChatRequest;
import com.studyassistant.dto.chat.ChatResponse;
import com.studyassistant.service.ai.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * REST controller for AI-powered question answering.
 *
 * <p>Intentionally thin – all business logic lives in {@link ChatService}.
 * The controller only binds the HTTP request, validates it, delegates,
 * and returns the response.
 *
 * <pre>
 * POST /api/chat
 * Content-Type: application/json
 * Body: { "uploadId": "...", "question": "..." }
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * Ask a question about an uploaded document.
     *
     * @param request validated chat request
     * @return 200 with {@link ChatResponse};
     *         400 if validation fails (handled by GlobalExceptionHandler);
     *         404 if no knowledge.json exists for the uploadId;
     *         503 if the AI provider is unavailable
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request) throws IOException {

        log.debug("POST /api/chat – uploadId='{}'", request.getUploadId());
        ChatResponse response = chatService.answer(request);
        return ResponseEntity.ok(response);
    }
}
