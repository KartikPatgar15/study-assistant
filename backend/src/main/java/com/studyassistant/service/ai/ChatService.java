package com.studyassistant.service.ai;

import com.studyassistant.dto.chat.ChatRequest;
import com.studyassistant.dto.chat.ChatResponse;
import com.studyassistant.dto.python.PythonChatResponse;
import com.studyassistant.service.python.PythonAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Chat service that delegates all AI processing to the Python AI service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final PythonAiService pythonAiService;

    public ChatResponse answer(ChatRequest request) {

        log.info(
                "Chat request - uploadId={}, questionLength={}",
                request.getUploadId(),
                request.getQuestion().length()
        );

        PythonChatResponse response =
                pythonAiService.ask(
                        request.getUploadId(),
                        request.getQuestion()
                );

        return ChatResponse.builder()
                .answer(response.getAnswer())
                .provider("python")
                .model("gemini-2.5-flash")
                .timestamp(Instant.now())
                .build();
    }
}