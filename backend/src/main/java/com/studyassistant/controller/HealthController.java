package com.studyassistant.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight liveness probe.
 *
 * <p>{@code GET /api/health} returns {@code {"status":"UP"}} and HTTP 200.
 * This endpoint is intentionally thin – it confirms the application context
 * started and the HTTP stack is reachable. Deep dependency checks (DB,
 * storage) are deferred to Spring Actuator's {@code /actuator/health}.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
