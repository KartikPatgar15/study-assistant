package com.studyassistant.dto;

/**
 * Response body returned by {@code POST /api/upload}.
 *
 * <p>Immutable by design – constructed via the static factory {@link #success}.
 * Future modules may add additional fields (e.g. {@code sessionId}) without
 * breaking existing consumers because JSON deserialisation ignores unknown fields
 * by default.
 *
 * <pre>
 * {
 *   "uploadId":        "3f2a1b...",
 *   "originalFileName":"notes.pdf",
 *   "storedFileName":  "3f2a1b..._notes.pdf",
 *   "fileSize":        1823445,
 *   "status":          "SUCCESS"
 * }
 * </pre>
 */
public class UploadResponse {

    private final String uploadId;
    private final String originalFileName;
    private final String storedFileName;
    private final long   fileSize;
    private final String status;

    private UploadResponse(String uploadId,
                           String originalFileName,
                           String storedFileName,
                           long fileSize,
                           String status) {
        this.uploadId         = uploadId;
        this.originalFileName = originalFileName;
        this.storedFileName   = storedFileName;
        this.fileSize         = fileSize;
        this.status           = status;
    }

    /** Builds a successful upload response. */
    public static UploadResponse success(String uploadId,
                                         String originalFileName,
                                         String storedFileName,
                                         long fileSize) {
        return new UploadResponse(uploadId, originalFileName, storedFileName, fileSize, "SUCCESS");
    }

    // ── Getters (required for Jackson serialisation) ─────────────────────────

    public String getUploadId()         { return uploadId; }
    public String getOriginalFileName() { return originalFileName; }
    public String getStoredFileName()   { return storedFileName; }
    public long   getFileSize()         { return fileSize; }
    public String getStatus()           { return status; }
}
