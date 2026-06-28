package com.studyassistant.service.processing;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages the {@code processed/} output directory tree.
 *
 * <p>Single Responsibility: filesystem layout. No PDF logic lives here.
 *
 * <p>For each upload the structure created is:
 * <pre>
 * processed/
 *   &lt;uploadId&gt;/
 *     text/
 *     images/
 * </pre>
 * {@code metadata.json} and {@code processing-report.json} are written
 * directly into {@code processed/<uploadId>/} by {@link DocumentProcessingService}.
 */
@Slf4j
@Service
public class ProcessedStorage {

    @Value("${app.processing.output-directory:processed}")
    private String outputDirectory;

    private Path processedRoot;

    @PostConstruct
    public void init() throws IOException {
        processedRoot = Paths.get(outputDirectory).toAbsolutePath().normalize();
        Files.createDirectories(processedRoot);
        log.info("Processed output directory ready: {}", processedRoot);
    }

    /**
     * Creates the full output directory tree for one upload and returns the root.
     *
     * @param uploadId the UUID assigned during upload
     * @return the absolute path to {@code processed/<uploadId>/}
     * @throws IOException if directories cannot be created
     */
    public Path createOutputDirectories(String uploadId) throws IOException {
        Path uploadOutput = processedRoot.resolve(uploadId);
        Files.createDirectories(uploadOutput.resolve("text"));
        Files.createDirectories(uploadOutput.resolve("images"));
        log.debug("Output directories created for uploadId={}", uploadId);
        return uploadOutput;
    }

    /**
     * Returns the root path for a previously processed upload.
     * Does not check whether the directory exists.
     */
    public Path getOutputRoot(String uploadId) {
        return processedRoot.resolve(uploadId);
    }

    public Path getProcessedRoot() {
        return processedRoot;
    }
}
