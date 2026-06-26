package com.studyassistant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test – verifies the Spring application context loads without errors.
 *
 * <p>Uses the "test" profile so a real database connection is not required.
 * An in-memory H2 datasource (or mock) should be configured in
 * {@code application-test.properties} when the JPA layer is exercised.
 */
@SpringBootTest
@ActiveProfiles("test")
class StudyAssistantApplicationTests {

    @Test
    void contextLoads() {
        // If the context fails to start, this test will fail with a descriptive error.
    }
}
