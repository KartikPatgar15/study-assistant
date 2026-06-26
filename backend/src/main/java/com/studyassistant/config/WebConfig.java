package com.studyassistant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * General web and serialisation configuration.
 *
 * <p>Registers the JavaTimeModule so that {@link java.time.Instant} and
 * {@link java.time.LocalDateTime} serialise as ISO-8601 strings rather than
 * numeric timestamps. Consistent date handling across the API matters from M01
 * so that future modules don't have to retrofit it.
 */
@Configuration
public class WebConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
