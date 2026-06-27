package com.studyassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Typed configuration for file upload behaviour.
 *
 * <p>All values are bound from the {@code app.upload.*} namespace in
 * {@code application.properties}, so limits and allowed types can be changed
 * without touching Java code.
 *
 * <p>Defaults are applied here so the application starts correctly with no
 * additional configuration, while still being overridable per environment.
 */
@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    /** Directory where uploaded files are stored (relative to working directory). */
    private String directory = "uploads";

    /** Maximum permitted file size in bytes. Default: 25 MB. */
    private long maxFileSizeBytes = 26_214_400L; // 25 * 1024 * 1024

    /**
     * MIME types accepted by the upload endpoint.
     * Checked in addition to the file extension for defence-in-depth.
     */
    private Set<String> allowedMimeTypes = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "image/png",
            "image/jpeg"
    );

    /**
     * File extensions accepted by the upload endpoint (lower-case, without the dot).
     * Validated independently of MIME type because browsers may send
     * {@code application/octet-stream} for uncommon types.
     */
    private Set<String> allowedExtensions = Set.of(
            "pdf", "docx", "txt", "png", "jpg", "jpeg"
    );

    // ── Getters & setters ────────────────────────────────────────────────────

    public String getDirectory() { return directory; }
    public void setDirectory(String directory) { this.directory = directory; }

    public long getMaxFileSizeBytes() { return maxFileSizeBytes; }
    public void setMaxFileSizeBytes(long maxFileSizeBytes) { this.maxFileSizeBytes = maxFileSizeBytes; }

    public Set<String> getAllowedMimeTypes() { return allowedMimeTypes; }
    public void setAllowedMimeTypes(Set<String> allowedMimeTypes) { this.allowedMimeTypes = allowedMimeTypes; }

    public Set<String> getAllowedExtensions() { return allowedExtensions; }
    public void setAllowedExtensions(Set<String> allowedExtensions) { this.allowedExtensions = allowedExtensions; }
}
