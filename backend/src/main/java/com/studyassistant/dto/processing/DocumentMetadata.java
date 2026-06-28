package com.studyassistant.dto.processing;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Serialised to {@code processed/<uploadId>/metadata.json}.
 *
 * <p>Immutable – constructed via {@link Builder}.
 * All timestamps serialise as ISO-8601 strings (configured globally in WebConfig).
 *
 * <p>M04 will read this file directly from disk to obtain document context
 * without re-processing the PDF.
 */
public class DocumentMetadata {

    private final String  uploadId;
    private final String  originalFileName;
    private final String  storedFileName;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final Instant uploadTimestamp;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final Instant processingTimestamp;
    private final long    processingDurationMs;
    private final int     totalPages;
    private final int     totalImages;
    private final long    fileSizeBytes;
    private final String  pdfVersion;

    private DocumentMetadata(Builder b) {
        this.uploadId             = b.uploadId;
        this.originalFileName     = b.originalFileName;
        this.storedFileName       = b.storedFileName;
        this.uploadTimestamp      = b.uploadTimestamp;
        this.processingTimestamp  = b.processingTimestamp;
        this.processingDurationMs = b.processingDurationMs;
        this.totalPages           = b.totalPages;
        this.totalImages          = b.totalImages;
        this.fileSizeBytes        = b.fileSizeBytes;
        this.pdfVersion           = b.pdfVersion;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String  getUploadId()             { return uploadId; }
    public String  getOriginalFileName()     { return originalFileName; }
    public String  getStoredFileName()       { return storedFileName; }
    public Instant getUploadTimestamp()      { return uploadTimestamp; }
    public Instant getProcessingTimestamp()  { return processingTimestamp; }
    public long    getProcessingDurationMs() { return processingDurationMs; }
    public int     getTotalPages()           { return totalPages; }
    public int     getTotalImages()          { return totalImages; }
    public long    getFileSizeBytes()        { return fileSizeBytes; }
    public String  getPdfVersion()           { return pdfVersion; }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String  uploadId;
        private String  originalFileName;
        private String  storedFileName;
        private Instant uploadTimestamp;
        private Instant processingTimestamp;
        private long    processingDurationMs;
        private int     totalPages;
        private int     totalImages;
        private long    fileSizeBytes;
        private String  pdfVersion;

        public Builder uploadId(String v)             { uploadId = v;             return this; }
        public Builder originalFileName(String v)     { originalFileName = v;     return this; }
        public Builder storedFileName(String v)       { storedFileName = v;       return this; }
        public Builder uploadTimestamp(Instant v)     { uploadTimestamp = v;      return this; }
        public Builder processingTimestamp(Instant v) { processingTimestamp = v;  return this; }
        public Builder processingDurationMs(long v)   { processingDurationMs = v; return this; }
        public Builder totalPages(int v)              { totalPages = v;           return this; }
        public Builder totalImages(int v)             { totalImages = v;          return this; }
        public Builder fileSizeBytes(long v)          { fileSizeBytes = v;        return this; }
        public Builder pdfVersion(String v)           { pdfVersion = v;           return this; }

        public DocumentMetadata build() { return new DocumentMetadata(this); }
    }
}
