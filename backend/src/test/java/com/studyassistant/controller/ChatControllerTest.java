package com.studyassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyassistant.dto.chat.ChatRequest;
import com.studyassistant.dto.chat.ChatResponse;
import com.studyassistant.exception.ResourceNotFoundException;
import com.studyassistant.service.ai.AiProviderException;
import com.studyassistant.service.ai.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller integration tests for {@link ChatController}.
 * Uses {@code @WebMvcTest} – no database, no real AI calls.
 */
@WebMvcTest(ChatController.class)
@ActiveProfiles("test")
class ChatControllerTest {

    @Autowired MockMvc     mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  ChatService  chatService;

    @Test
    void chat_validRequest_returns200WithAnswer() throws Exception {
        ChatResponse fakeResponse = ChatResponse.builder()
                .answer("Memory management controls allocation.")
                .provider("gemini")
                .model("gemini-2.5-flash")
                .timestamp(Instant.now())
                .build();

        when(chatService.answer(any())).thenReturn(fakeResponse);

        ChatRequest req = new ChatRequest();
        req.setUploadId("test-upload-id");
        req.setQuestion("What is memory management?");

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Memory management controls allocation."))
                .andExpect(jsonPath("$.provider").value("gemini"))
                .andExpect(jsonPath("$.model").value("gemini-2.5-flash"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void chat_blankUploadId_returns400() throws Exception {
        ChatRequest req = new ChatRequest();
        req.setUploadId("");
        req.setQuestion("A question");

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_blankQuestion_returns400() throws Exception {
        ChatRequest req = new ChatRequest();
        req.setUploadId("some-id");
        req.setQuestion("");

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_unknownUploadId_returns404() throws Exception {
        when(chatService.answer(any()))
                .thenThrow(new ResourceNotFoundException("No knowledge found for uploadId 'bad-id'"));

        ChatRequest req = new ChatRequest();
        req.setUploadId("bad-id");
        req.setQuestion("Any question?");

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void chat_aiProviderFailure_returns503() throws Exception {
        when(chatService.answer(any()))
                .thenThrow(new AiProviderException("Gemini is unavailable"));

        ChatRequest req = new ChatRequest();
        req.setUploadId("some-id");
        req.setQuestion("Some question?");

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("Gemini is unavailable"));
    }
}
