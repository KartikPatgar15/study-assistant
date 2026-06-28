package com.studyassistant.service.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyassistant.dto.processing.DocumentMetadata;
import com.studyassistant.dto.processing.ProcessingReport;
import com.studyassistant.dto.processing.ProcessingResult;
import com.studyassistant.model.ProcessingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Core document processing pipeline for PDF files.
 *
 * <p>Pipeline stages (executed synchronously within the upload request):
 * <ol>
 *   <li>Create output directory structure under {@code processed/<uploadId>/}</li>
 *   <li>Load and validate the PDF with PDFBox</li>
 *   <li>Extract per-page text → {@code text/page-N.txt}</li>
 *   <li>Extract embedded images → {@code images/page-N-img-M.<ext>}</li>
 *   <li>Write {@code metadata.json}</li>
 *   <li>Write {@code processing-report.json}</li>
 * </ol>
 *
 * <p>All recoverable errors (missing text, unreadable images) are captured as
 * warnings in the report rather than aborting the whole pipeline.
 * Only truly fatal errors (corrupt/encrypted PDF, I/O failure) propagate up.
 *
 * <p>Single Responsibility: this service only processes PDFs. Non-PDF files
 * uploaded via M02 bypass processing gracefully (a no-op result is returned).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final ObjectMapper     objectMapper;
    private final ProcessedStorage processedStorage;

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Entry point called by {@link com.studyassistant.service.UploadService}
     * immediately after a file is stored to disk.
     *
     * @param uploadId         the UUID assigned during upload
     * @param originalFileName the user-facing filename (e.g. "notes.pdf")
     * @param storedFileName   the on-disk filename (e.g. "uuid_notes.pdf")
     * @param storedFilePath   absolute path to the uploaded file
     * @param fileSizeBytes    file size in bytes
     * @param uploadTimestamp  when the file was received
     * @return lightweight {@link ProcessingResult} summary for the API response
     */
    public ProcessingResult process(String uploadId,
                                    String originalFileName,
                                    String storedFileName,
                                    Path   storedFilePath,
                                    long   fileSizeBytes,
                                    Instant uploadTimestamp) {

        log.info("Processing started – uploadId={}, file='{}'", uploadId, originalFileName);

        String extension = extractExtension(originalFileName).toLowerCase();
        if (!"pdf".equals(extension)) {
            log.info("Non-PDF file ('{}') – skipping document processing", extension);
            return ProcessingResult.builder()
                    .status(ProcessingStatus.SUCCESS)
                    .message("File stored successfully. Document intelligence is available for PDF files only.")
                    .build();
        }

        Instant processingStart = Instant.now();

        // Hoisted outside the try block so the catch block can reference outputRoot
        // when writing the failure report.
        Path outputRoot = null;

        List<String> warnings          = new ArrayList<>();
        List<String> recoverableErrors = new ArrayList<>();
        int          pagesProcessed    = 0;
        int          totalImages       = 0;
        String       pdfVersion        = "unknown";

        try {
            outputRoot = processedStorage.createOutputDirectories(uploadId);
            Path textDir   = outputRoot.resolve("text");
            Path imagesDir = outputRoot.resolve("images");

            try (PDDocument document = Loader.loadPDF(storedFilePath.toFile())) {

                // ── Encrypted PDF guard ───────────────────────────────────────
                if (document.isEncrypted()) {
                    log.warn("PDF is encrypted – uploadId={}", uploadId);
                    return failedResult(uploadId, originalFileName, storedFileName,
                            fileSizeBytes, uploadTimestamp, processingStart,
                            "PDF is encrypted and cannot be processed.", outputRoot, objectMapper);
                }

                int pageCount = document.getNumberOfPages();
                log.info("PDF loaded – uploadId={}, pages={}", uploadId, pageCount);

                // ── PDF version (PDFBox 3.x API) ──────────────────────────────
                try {
                    pdfVersion = String.valueOf(document.getVersion());
                } catch (Exception e) {
                    warnings.add("Could not read PDF version: " + e.getMessage());
                }

                // ── Text extraction ───────────────────────────────────────────
                log.info("Extracting text – uploadId={}", uploadId);
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);

                for (int pageNum = 1; pageNum <= pageCount; pageNum++) {
                    try {
                        stripper.setStartPage(pageNum);
                        stripper.setEndPage(pageNum);
                        String rawText = stripper.getText(document);
                        String cleaned = cleanText(rawText);

                        Path pageFile = textDir.resolve("page-" + pageNum + ".txt");
                        Files.writeString(pageFile, cleaned, StandardCharsets.UTF_8);
                        pagesProcessed++;

                        if (cleaned.isBlank()) {
                            warnings.add("Page " + pageNum + " contains no extractable text (may be image-only).");
                        }
                    } catch (Exception e) {
                        recoverableErrors.add("Failed to extract text from page " + pageNum + ": " + e.getMessage());
                        log.warn("Text extraction failed on page {} – uploadId={}", pageNum, uploadId, e);
                    }
                }

                // ── Image extraction ──────────────────────────────────────────
                log.info("Extracting images – uploadId={}", uploadId);
                for (int pageNum = 1; pageNum <= pageCount; pageNum++) {
                    try {
                        PDPage      page      = document.getPage(pageNum - 1);
                        PDResources resources = page.getResources();
                        if (resources == null) continue;

                        int imgIndexOnPage = 0;
                        for (var name : resources.getXObjectNames()) {
                            PDXObject xObject;
                            try {
                                xObject = resources.getXObject(name);
                            } catch (Exception e) {
                                recoverableErrors.add("Could not read XObject on page " + pageNum
                                                      + ": " + e.getMessage());
                                continue;
                            }

                            if (!(xObject instanceof PDImageXObject imageXObject)) continue;

                            imgIndexOnPage++;
                            String imageFormat   = resolveImageFormat(imageXObject);
                            String imageFilename = String.format("page-%d-img-%d.%s",
                                    pageNum, imgIndexOnPage, imageFormat);
                            Path   imagePath     = imagesDir.resolve(imageFilename);

                            try {
                                BufferedImage bufferedImage = imageXObject.getImage();
                                if (bufferedImage != null) {
                                    ImageIO.write(bufferedImage, imageFormat, imagePath.toFile());
                                    totalImages++;
                                }
                            } catch (Exception e) {
                                recoverableErrors.add("Could not write image " + imageFilename
                                                      + ": " + e.getMessage());
                                log.warn("Image write failed – {} – uploadId={}", imageFilename, uploadId, e);
                            }
                        }
                    } catch (Exception e) {
                        recoverableErrors.add("Image extraction failed for page " + pageNum
                                             + ": " + e.getMessage());
                        log.warn("Image extraction error on page {} – uploadId={}", pageNum, uploadId, e);
                    }
                }

                // ── Determine final status ────────────────────────────────────
                ProcessingStatus finalStatus;
                if (!recoverableErrors.isEmpty()) {
                    finalStatus = ProcessingStatus.PARTIAL_SUCCESS;
                } else if (pagesProcessed == 0) {
                    finalStatus = ProcessingStatus.PARTIAL_SUCCESS;
                    warnings.add("No pages could be processed.");
                } else {
                    finalStatus = ProcessingStatus.SUCCESS;
                }

                long durationMs = Instant.now().toEpochMilli() - processingStart.toEpochMilli();

                // ── Write metadata.json ───────────────────────────────────────
                log.info("Writing files – uploadId={}", uploadId);
                DocumentMetadata metadata = DocumentMetadata.builder()
                        .uploadId(uploadId)
                        .originalFileName(originalFileName)
                        .storedFileName(storedFileName)
                        .uploadTimestamp(uploadTimestamp)
                        .processingTimestamp(processingStart)
                        .processingDurationMs(durationMs)
                        .totalPages(pageCount)
                        .totalImages(totalImages)
                        .fileSizeBytes(fileSizeBytes)
                        .pdfVersion(pdfVersion)
                        .build();

                objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValue(outputRoot.resolve("metadata.json").toFile(), metadata);

                // ── Write processing-report.json ──────────────────────────────
                ProcessingReport report = ProcessingReport.builder()
                        .status(finalStatus)
                        .pagesProcessed(pagesProcessed)
                        .imagesExtracted(totalImages)
                        .warnings(warnings)
                        .recoverableErrors(recoverableErrors)
                        .processingDurationMs(durationMs)
                        .completionTimestamp(Instant.now())
                        .build();

                objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValue(outputRoot.resolve("processing-report.json").toFile(), report);

                log.info("Processing completed – uploadId={}, status={}, pages={}, images={}, duration={}ms",
                        uploadId, finalStatus, pageCount, totalImages, durationMs);

                return ProcessingResult.builder()
                        .status(finalStatus)
                        .totalPages(pageCount)
                        .totalImages(totalImages)
                        .processingDurationMs(durationMs)
                        .message(buildSummaryMessage(finalStatus, pageCount, totalImages, warnings, recoverableErrors))
                        .build();
            }

        } catch (IOException e) {
            log.error("Fatal processing error – uploadId={}, file='{}'", uploadId, originalFileName, e);

            // Attempt to write a FAILED report so the output directory isn't empty.
            // outputRoot may be null if createOutputDirectories itself failed.
            if (outputRoot != null) {
                try {
                    long durationMs = Instant.now().toEpochMilli() - processingStart.toEpochMilli();
                    ProcessingReport failReport = ProcessingReport.builder()
                            .status(ProcessingStatus.FAILED)
                            .pagesProcessed(0)
                            .imagesExtracted(0)
                            .recoverableErrors(List.of("Fatal error: " + e.getMessage()))
                            .processingDurationMs(durationMs)
                            .completionTimestamp(Instant.now())
                            .build();
                    objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValue(outputRoot.resolve("processing-report.json").toFile(), failReport);
                } catch (IOException reportEx) {
                    log.error("Could not write failure report – uploadId={}", uploadId, reportEx);
                }
            }

            return ProcessingResult.builder()
                    .status(ProcessingStatus.FAILED)
                    .message("Document could not be processed: " + sanitiseErrorMessage(e))
                    .build();
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    /**
     * Normalise whitespace while preserving paragraph breaks.
     */
    private String cleanText(String raw) {
        if (raw == null) return "";
        return raw
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    /**
     * Resolve the output image format. Falls back to PNG.
     */
    private String resolveImageFormat(PDImageXObject image) {
        String suffix = image.getSuffix();
        if (suffix == null || suffix.isBlank()) return "png";
        return switch (suffix.toLowerCase()) {
            case "jpg", "jpeg" -> "jpg";
            case "png"         -> "png";
            case "gif"         -> "gif";
            case "tiff", "tif" -> "tiff";
            default            -> "png";
        };
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0 && dot < filename.length() - 1) ? filename.substring(dot + 1) : "";
    }

    private String sanitiseErrorMessage(IOException e) {
        String msg = e.getMessage();
        if (msg == null) return "Unknown I/O error.";
        if (msg.contains("encrypted") || msg.contains("password")) return "PDF is encrypted.";
        if (msg.contains("truncated") || msg.contains("corrupt"))  return "PDF appears to be corrupted.";
        return "The document could not be read.";
    }

    private String buildSummaryMessage(ProcessingStatus status, int pages, int images,
                                       List<String> warnings, List<String> errors) {
        return switch (status) {
            case SUCCESS         -> String.format("Processed %d page%s, extracted %d image%s.",
                                         pages,  pages  == 1 ? "" : "s",
                                         images, images == 1 ? "" : "s");
            case PARTIAL_SUCCESS -> String.format(
                                         "Processed with %d warning%s and %d recoverable error%s.",
                                         warnings.size(), warnings.size() == 1 ? "" : "s",
                                         errors.size(),   errors.size()   == 1 ? "" : "s");
            case FAILED          -> "Processing failed. The document may be corrupted or encrypted.";
        };
    }

    private ProcessingResult failedResult(String uploadId,
                                          String originalFileName,
                                          String storedFileName,
                                          long fileSizeBytes,
                                          Instant uploadTimestamp,
                                          Instant processingStart,
                                          String reason,
                                          Path outputRoot,
                                          ObjectMapper mapper) {
        long durationMs = Instant.now().toEpochMilli() - processingStart.toEpochMilli();
        try {
            ProcessingReport report = ProcessingReport.builder()
                    .status(ProcessingStatus.FAILED)
                    .recoverableErrors(List.of(reason))
                    .processingDurationMs(durationMs)
                    .completionTimestamp(Instant.now())
                    .build();
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(outputRoot.resolve("processing-report.json").toFile(), report);
        } catch (IOException ex) {
            log.error("Could not write failure report for uploadId={}", uploadId, ex);
        }
        return ProcessingResult.builder()
                .status(ProcessingStatus.FAILED)
                .processingDurationMs(durationMs)
                .message(reason)
                .build();
    }
}
