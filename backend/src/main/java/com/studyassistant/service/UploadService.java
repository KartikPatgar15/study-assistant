package com.studyassistant.service;

import com.studyassistant.config.UploadProperties;
import com.studyassistant.dto.UploadResponse;
import com.studyassistant.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Handles all file upload business logic.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Ensure the upload directory exists on startup.</li>
 *   <li>Validate file size, presence, and type.</li>
 *   <li>Generate a collision-safe stored filename.</li>
 *   <li>Write the file to local disk.</li>
 *   <li>Return a structured {@link UploadResponse}.</li>
 * </ol>
 *
 * <p>No database interaction occurs here – that belongs to a future module.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    private final UploadProperties uploadProperties;

    private Path uploadDirectory;

    /**
     * Resolves and creates the upload directory on application startup.
     * Using {@code @PostConstruct} keeps the side-effect explicit and testable.
     */
    @PostConstruct
    void initUploadDirectory() throws IOException {
        uploadDirectory = Paths.get(uploadProperties.getDirectory()).toAbsolutePath().normalize();
        Files.createDirectories(uploadDirectory);
        log.info("Upload directory ready: {}", uploadDirectory);
    }

    /**
     * Validates and stores the uploaded file.
     *
     * @param file the multipart file received from the HTTP request
     * @return {@link UploadResponse} describing the stored file
     * @throws BadRequestException if the file fails any validation rule
     * @throws IOException         if the file cannot be written to disk
     */
    public UploadResponse store(MultipartFile file) throws IOException {

        // 1. Must not be empty
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was provided or the file is empty.");
        }

        // 2. Size check (defence-in-depth; Spring's multipart limit is also configured)
        if (file.getSize() > uploadProperties.getMaxFileSizeBytes()) {
            throw new BadRequestException(
                    String.format("File size %d bytes exceeds the maximum allowed size of %d bytes (25 MB).",
                            file.getSize(), uploadProperties.getMaxFileSizeBytes()));
        }

        // 3. Resolve and clean the original filename
        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload"
        );

        // 4. Extension check
        String extension = extractExtension(originalFilename).toLowerCase();
        if (!uploadProperties.getAllowedExtensions().contains(extension)) {
            throw new BadRequestException(
                    String.format("File type '.%s' is not allowed. Accepted types: %s",
                            extension, uploadProperties.getAllowedExtensions()));
        }

        // 5. MIME type check
        String contentType = file.getContentType() != null ? file.getContentType() : "";
        if (!uploadProperties.getAllowedMimeTypes().contains(contentType)) {
            throw new BadRequestException(
                    String.format("Content type '%s' is not allowed.", contentType));
        }

        // 6. Path traversal guard
        if (originalFilename.contains("..")) {
            throw new BadRequestException("Filename contains invalid path sequence '..'");
        }

        // 7. Generate unique stored filename and write to disk
        String uploadId      = UUID.randomUUID().toString();
        String storedFilename = uploadId + "_" + originalFilename;
        Path   targetPath    = uploadDirectory.resolve(storedFilename);

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored file '{}' as '{}' ({} bytes)", originalFilename, storedFilename, file.getSize());

        return UploadResponse.success(uploadId, originalFilename, storedFilename, file.getSize());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1);
    }
}
