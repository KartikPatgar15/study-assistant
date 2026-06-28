package com.studyassistant.service;

import com.studyassistant.config.UploadProperties;
import com.studyassistant.dto.UploadResponse;
import com.studyassistant.dto.knowledge.KnowledgeSummary;
import com.studyassistant.dto.processing.ProcessingResult;
import com.studyassistant.exception.BadRequestException;
import com.studyassistant.model.ProcessingStatus;
import com.studyassistant.service.knowledge.KnowledgeBuilderService;
import com.studyassistant.service.processing.DocumentProcessingService;
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
import java.time.Instant;
import java.util.UUID;

/**
 * Handles file upload storage and orchestrates the processing pipeline.
 *
 * <p>Pipeline (all synchronous within the upload request):
 * <ol>
 *   <li>Validate and store the file to disk (M02).</li>
 *   <li>Extract text, images, metadata, and report via {@link DocumentProcessingService} (M03).</li>
 *   <li>Build semantic knowledge chunks via {@link KnowledgeBuilderService} (M03.5).</li>
 *   <li>Return a combined {@link UploadResponse}.</li>
 * </ol>
 *
 * <p>Knowledge building is skipped (with a warning) if M03 processing failed,
 * so a corrupt/encrypted PDF never reaches the chunker.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    private final UploadProperties          uploadProperties;
    private final DocumentProcessingService documentProcessingService;
    private final KnowledgeBuilderService   knowledgeBuilderService;

    private Path uploadDirectory;

    @PostConstruct
    void initUploadDirectory() throws IOException {
        uploadDirectory = Paths.get(uploadProperties.getDirectory()).toAbsolutePath().normalize();
        Files.createDirectories(uploadDirectory);
        log.info("Upload directory ready: {}", uploadDirectory);
    }

    /**
     * Validates, stores, processes, and builds knowledge for the uploaded file.
     *
     * @param file the multipart file received from the HTTP request
     * @return {@link UploadResponse} with full pipeline result
     * @throws BadRequestException if the file fails any validation rule
     * @throws IOException         if the file cannot be written to disk
     */
    public UploadResponse store(MultipartFile file) throws IOException {

        // 1. Must not be empty
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was provided or the file is empty.");
        }

        // 2. Size check
        if (file.getSize() > uploadProperties.getMaxFileSizeBytes()) {
            throw new BadRequestException(
                    String.format("File size %d bytes exceeds the maximum allowed size of %d bytes (25 MB).",
                            file.getSize(), uploadProperties.getMaxFileSizeBytes()));
        }

        // 3. Clean filename
        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload");

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

        // 7. Store file
        Instant uploadTimestamp = Instant.now();
        String  uploadId        = UUID.randomUUID().toString();
        String  storedFilename  = uploadId + "_" + originalFilename;
        Path    targetPath      = uploadDirectory.resolve(storedFilename);

        log.info("Upload received – uploadId={}, file='{}', size={} bytes",
                uploadId, originalFilename, file.getSize());

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored file '{}' as '{}' ({} bytes)", originalFilename, storedFilename, file.getSize());

        // 8. M03 – document processing (text + image extraction)
        ProcessingResult processingResult = documentProcessingService.process(
                uploadId, originalFilename, storedFilename,
                targetPath, file.getSize(), uploadTimestamp);

        // 9. M03.5 – knowledge build (chunking + image association + keyword extraction)
        //    Only run if M03 succeeded or partially succeeded for a PDF file.
        KnowledgeSummary knowledgeSummary = null;
        if (processingResult.getStatus() != ProcessingStatus.FAILED
                && processingResult.getTotalPages() > 0) {
            try {
                knowledgeSummary = knowledgeBuilderService.build(uploadId);
            } catch (IOException e) {
                // Knowledge build failure is non-fatal: the document is already stored
                // and M03 output is intact. Log at ERROR and continue.
                log.error("Knowledge build failed for uploadId={} – pipeline continues without it",
                        uploadId, e);
            }
        } else {
            log.debug("Knowledge build skipped for uploadId={} (status={}, pages={})",
                    uploadId, processingResult.getStatus(), processingResult.getTotalPages());
        }

        // 10. Compose final response
        ProcessingResult finalResult = ProcessingResult.builder()
                .status(processingResult.getStatus())
                .totalPages(processingResult.getTotalPages())
                .totalImages(processingResult.getTotalImages())
                .processingDurationMs(processingResult.getProcessingDurationMs())
                .message(processingResult.getMessage())
                .knowledge(knowledgeSummary)
                .build();

        return UploadResponse.success(uploadId, originalFilename, storedFilename,
                file.getSize(), finalResult);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex >= 0 && dotIndex < filename.length() - 1)
                ? filename.substring(dotIndex + 1)
                : "";
    }
}
