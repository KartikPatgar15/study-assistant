package com.studyassistant.service.python;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class PythonAiClient {

    private final WebClient.Builder webClientBuilder;

    public String health() {
        return webClientBuilder.build()
                .get()
                .uri("http://localhost:8001/health")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}