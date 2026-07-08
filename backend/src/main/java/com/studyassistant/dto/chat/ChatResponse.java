package com.studyassistant.dto.chat;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Response body returned by {@code POST /api/chat}.
 *
 * <p>Immutable – constructed via {@link Builder}.
 * Includes provider metadata so the frontend can display which model answered.
 */
public class ChatResponse {

    private final String  answer;
    private final String  provider;
    private final String  model;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final Instant timestamp;

    private ChatResponse(Builder b) {
        this.answer    = b.answer;
        this.provider  = b.provider;
        this.model     = b.model;
        this.timestamp = b.timestamp;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String  getAnswer()    { return answer; }
    public String  getProvider()  { return provider; }
    public String  getModel()     { return model; }
    public Instant getTimestamp() { return timestamp; }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String  answer    = "";
        private String  provider  = "";
        private String  model     = "";
        private Instant timestamp = Instant.now();

        public Builder answer(String v)    { answer = v;    return this; }
        public Builder provider(String v)  { provider = v;  return this; }
        public Builder model(String v)     { model = v;     return this; }
        public Builder timestamp(Instant v){ timestamp = v; return this; }

        public ChatResponse build() { return new ChatResponse(this); }
    }
}
