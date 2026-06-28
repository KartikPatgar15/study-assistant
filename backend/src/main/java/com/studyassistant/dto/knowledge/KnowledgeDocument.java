package com.studyassistant.dto.knowledge;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;

/**
 * The root object serialised to {@code processed/<uploadId>/knowledge.json}.
 *
 * <p>This file is the single source of truth for M04. The AI engine reads it,
 * selects the most relevant {@link KnowledgeChunk}s, and sends their text to
 * the chosen AI provider. No re-processing of the original PDF is needed.
 *
 * <p>Structure:
 * <pre>
 * {
 *   "uploadId":        "…",
 *   "language":        "en",
 *   "createdTimestamp": "…",
 *   "totalChunks":     12,
 *   "chunks":          [ … ],
 *   "summary":         { … }
 * }
 * </pre>
 */
public class KnowledgeDocument {

    private final String            uploadId;

    /**
     * BCP-47 language tag detected from the document content.
     * Simple heuristic detection — not AI-based.
     * Defaults to {@code "en"} when detection is inconclusive.
     * Included for future multilingual AI provider routing.
     */
    private final String            language;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final Instant           createdTimestamp;

    private final int               totalChunks;
    private final List<KnowledgeChunk> chunks;
    private final KnowledgeSummary  summary;

    private KnowledgeDocument(Builder b) {
        this.uploadId         = b.uploadId;
        this.language         = b.language;
        this.createdTimestamp = b.createdTimestamp;
        this.totalChunks      = b.chunks.size();
        this.chunks           = List.copyOf(b.chunks);
        this.summary          = b.summary;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String               getUploadId()         { return uploadId; }
    public String               getLanguage()         { return language; }
    public Instant              getCreatedTimestamp() { return createdTimestamp; }
    public int                  getTotalChunks()      { return totalChunks; }
    public List<KnowledgeChunk> getChunks()           { return chunks; }
    public KnowledgeSummary     getSummary()          { return summary; }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String               uploadId         = "";
        private String               language         = "en";
        private Instant              createdTimestamp = Instant.now();
        private List<KnowledgeChunk> chunks           = List.of();
        private KnowledgeSummary     summary          = KnowledgeSummary.builder().build();

        public Builder uploadId(String v)              { uploadId = v;         return this; }
        public Builder language(String v)              { language = v;         return this; }
        public Builder createdTimestamp(Instant v)     { createdTimestamp = v; return this; }
        public Builder chunks(List<KnowledgeChunk> v)  { chunks = v;           return this; }
        public Builder summary(KnowledgeSummary v)     { summary = v;          return this; }

        public KnowledgeDocument build() { return new KnowledgeDocument(this); }
    }
}
