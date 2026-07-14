package com.studyassistant.service.python;

import com.studyassistant.dto.python.PythonChatRequest;
import com.studyassistant.dto.python.PythonChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class PythonAiService {

    private final WebClient.Builder builder;

    public PythonChatResponse ask(String uploadId,
                                  String question) {

        PythonChatRequest request =
                PythonChatRequest.builder()
                        .uploadId(uploadId)
                        .question(question)
                        .build();

        return builder.build()
                .post()
                .uri("http://localhost:8001/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PythonChatResponse.class)
                .block();
    }
}