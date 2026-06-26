package com.studyassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the AI Study Assistant backend.
 *
 * <p>Spring Boot auto-configuration picks up all beans within the
 * {@code com.studyassistant} package hierarchy, including:
 * <ul>
 *   <li>{@code config}   – cross-cutting concerns (CORS, Jackson, etc.)</li>
 *   <li>{@code controller} – REST endpoints</li>
 *   <li>{@code exception} – global error handling</li>
 *   <li>{@code service}  – business logic (added in future modules)</li>
 *   <li>{@code model}    – JPA entities and DTOs</li>
 * </ul>
 */
@SpringBootApplication
public class StudyAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyAssistantApplication.class, args);
    }
}
