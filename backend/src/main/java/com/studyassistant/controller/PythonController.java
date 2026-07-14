package com.studyassistant.controller;

import com.studyassistant.service.python.PythonAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PythonController {

    private final PythonAiClient pythonAiClient;

    @GetMapping("/api/python/health")
    public String health() {
        return pythonAiClient.health();
    }
}