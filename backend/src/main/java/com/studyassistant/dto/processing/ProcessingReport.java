package com.studyassistant.dto.processing;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.studyassistant.model.ProcessingStatus;

import java.time.Instant;
import java.util.List;

/**
 * Serialised to {@code processed/<uploadId>/processing-report.json}.
 *
 * <p>Provides a complete audit trail of the processing run so that M04
 * can decide how to handle documents with warnings or partial failures.
 */
public class ProcessingReport {

    private final ProcessingStatus status;
    private final int              pagesProcessed;
    private final int              imagesExtracted;
    private final List<String>     warnings;
    private final List<String>     recoverableErrors;
    private final long             processingDurationMs;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final Instant          completionTimestamp;

    private ProcessingReport(Builder b) {
        this.status               = b.status;
        this.pagesProcessed       = b.pagesProcessed;
        this.imagesExtracted      = b.imagesExtracted;
        this.warnings             = List.copyOf(b.warnings);
        this.recoverableErrors    = List.copyOf(b.recoverableErrors);
        this.processingDurationMs = b.processingDurationMs;
        this.completionTimestamp  = b.completionTimestamp;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public ProcessingStatus getStatus()               { return status; }
    public int              getPagesProcessed()        { return pagesProcessed; }
    public int              getImagesExtracted()       { return imagesExtracted; }
    public List<String>     getWarnings()              { return warnings; }
    public List<String>     getRecoverableErrors()     { return recoverableErrors; }
    public long             getProcessingDurationMs()  { return processingDurationMs; }
    public Instant          getCompletionTimestamp()   { return completionTimestamp; }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private ProcessingStatus status               = ProcessingStatus.SUCCESS;
        private int              pagesProcessed       = 0;
        private int              imagesExtracted      = 0;
        private List<String>     warnings             = List.of();
        private List<String>     recoverableErrors    = List.of();
        private long             processingDurationMs = 0;
        private Instant          completionTimestamp  = Instant.now();

        public Builder status(ProcessingStatus v)            { status = v;               return this; }
        public Builder pagesProcessed(int v)                 { pagesProcessed = v;       return this; }
        public Builder imagesExtracted(int v)                { imagesExtracted = v;      return this; }
        public Builder warnings(List<String> v)              { warnings = v;             return this; }
        public Builder recoverableErrors(List<String> v)     { recoverableErrors = v;    return this; }
        public Builder processingDurationMs(long v)          { processingDurationMs = v; return this; }
        public Builder completionTimestamp(Instant v)        { completionTimestamp = v;  return this; }

        public ProcessingReport build() { return new ProcessingReport(this); }
    }
}
