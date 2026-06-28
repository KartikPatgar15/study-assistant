package com.studyassistant.dto;

import com.studyassistant.dto.processing.ProcessingResult;

/**
 * Response body returned by {@code POST /api/upload}.
 *
 * <p>Extended in M03 to include a {@link ProcessingResult} summary so the
 * frontend can display page count, image count, and processing status without
 * a second round-trip.
 *
 * <pre>
 * {
 *   "uploadId":        "3f2a1b...",
 *   "originalFileName":"notes.pdf",
 *   "storedFileName":  "3f2a1b..._notes.pdf",
 *   "fileSize":        1823445,
 *   "status":          "SUCCESS",
 *   "processing": {
 *     "status":               "SUCCESS",
 *     "totalPages":           12,
 *     "totalImages":          3,
 *     "processingDurationMs": 842,
 *     "message":              "Processed 12 pages, extracted 3 images."
 *   }
 * }
 * </pre>
 */
public class UploadResponse {

    private final String           uploadId;
    private final String           originalFileName;
    private final String           storedFileName;
    private final long             fileSize;
    private final String           status;
    private final ProcessingResult processing;

    private UploadResponse(String uploadId,
                           String originalFileName,
                           String storedFileName,
                           long fileSize,
                           String status,
                           ProcessingResult processing) {
        this.uploadId         = uploadId;
        this.originalFileName = originalFileName;
        this.storedFileName   = storedFileName;
        this.fileSize         = fileSize;
        this.status           = status;
        this.processing       = processing;
    }

    /** Builds a successful upload response (M01/M02 compatible – processing is null). */
    public static UploadResponse success(String uploadId,
                                         String originalFileName,
                                         String storedFileName,
                                         long fileSize) {
        return new UploadResponse(uploadId, originalFileName, storedFileName, fileSize, "SUCCESS", null);
    }

    /** Builds a successful upload response with processing result (M03+). */
    public static UploadResponse success(String uploadId,
                                         String originalFileName,
                                         String storedFileName,
                                         long fileSize,
                                         ProcessingResult processing) {
        return new UploadResponse(uploadId, originalFileName, storedFileName, fileSize, "SUCCESS", processing);
    }

    // ── Getters (required for Jackson serialisation) ─────────────────────────

    public String           getUploadId()         { return uploadId; }
    public String           getOriginalFileName() { return originalFileName; }
    public String           getStoredFileName()   { return storedFileName; }
    public long             getFileSize()         { return fileSize; }
    public String           getStatus()           { return status; }
    public ProcessingResult getProcessing()       { return processing; }
}
