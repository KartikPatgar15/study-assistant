package com.studyassistant.dto.processing;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.studyassistant.dto.knowledge.KnowledgeSummary;
import com.studyassistant.model.ProcessingStatus;

/**
 * Lightweight summary included in the {@code POST /api/upload} response.
 *
 * <p>Extended in M03.5 to include a nullable {@link KnowledgeSummary} field.
 * Null is omitted from JSON so M01/M02/M03 consumers remain unaffected.
 *
 * <p>Full processing detail is available on disk:
 * <ul>
 *   <li>{@code processed/<uploadId>/metadata.json}</li>
 *   <li>{@code processed/<uploadId>/processing-report.json}</li>
 *   <li>{@code processed/<uploadId>/knowledge.json}</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessingResult {

    private final ProcessingStatus status;
    private final int              totalPages;
    private final int              totalImages;
    private final long             processingDurationMs;
    private final String           message;

    /** Null for non-PDF files and for uploads where knowledge build was skipped. */
    private final KnowledgeSummary knowledge;

    private ProcessingResult(Builder b) {
        this.status               = b.status;
        this.totalPages           = b.totalPages;
        this.totalImages          = b.totalImages;
        this.processingDurationMs = b.processingDurationMs;
        this.message              = b.message;
        this.knowledge            = b.knowledge;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public ProcessingStatus getStatus()               { return status; }
    public int              getTotalPages()            { return totalPages; }
    public int              getTotalImages()           { return totalImages; }
    public long             getProcessingDurationMs()  { return processingDurationMs; }
    public String           getMessage()               { return message; }
    public KnowledgeSummary getKnowledge()             { return knowledge; }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private ProcessingStatus status               = ProcessingStatus.SUCCESS;
        private int              totalPages           = 0;
        private int              totalImages          = 0;
        private long             processingDurationMs = 0;
        private String           message              = "";
        private KnowledgeSummary knowledge            = null;

        public Builder status(ProcessingStatus v)           { status = v;               return this; }
        public Builder totalPages(int v)                    { totalPages = v;           return this; }
        public Builder totalImages(int v)                   { totalImages = v;          return this; }
        public Builder processingDurationMs(long v)         { processingDurationMs = v; return this; }
        public Builder message(String v)                    { message = v;             return this; }
        public Builder knowledge(KnowledgeSummary v)        { knowledge = v;           return this; }

        public ProcessingResult build() { return new ProcessingResult(this); }
    }
}
