package com.studyassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.studyassistant.dto.processing.DocumentMetadata;
import com.studyassistant.dto.processing.ProcessingReport;
import com.studyassistant.dto.processing.ProcessingResult;
import com.studyassistant.model.ProcessingStatus;
import com.studyassistant.service.processing.DocumentProcessingService;
import com.studyassistant.service.processing.ProcessedStorage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DocumentProcessingService}.
 *
 * <p>All tests use {@code @TempDir} so no real filesystem state is left behind.
 * PDFBox creates real (minimal) PDFs in memory so we exercise the actual
 * text/image extraction code paths.
 */
class DocumentProcessingServiceTest {

    @TempDir
    Path tempDir;

    private DocumentProcessingService service;
    private ObjectMapper              objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        ProcessedStorage storage = new ProcessedStorage();
        ReflectionTestUtils.setField(storage, "outputDirectory", tempDir.resolve("processed").toString());
        storage.init();

        service = new DocumentProcessingService(objectMapper, storage);
    }

    // ── Text extraction ───────────────────────────────────────────────────────

    @Test
    void textExtraction_singlePage_writesPageFile() throws Exception {
        Path pdfPath = createPdfWithText(tempDir, "hello-world.pdf", "Hello, World!");
        String uploadId = "test-upload-001";

        ProcessingResult result = service.process(
                uploadId, "hello-world.pdf", uploadId + "_hello-world.pdf",
                pdfPath, Files.size(pdfPath), Instant.now());

        assertThat(result.getStatus()).isEqualTo(ProcessingStatus.SUCCESS);
        assertThat(result.getTotalPages()).isEqualTo(1);

        Path pageFile = tempDir.resolve("processed/" + uploadId + "/text/page-1.txt");
        assertThat(pageFile).exists();
        assertThat(Files.readString(pageFile)).contains("Hello, World!");
    }

    @Test
    void textExtraction_multiPage_writesOneFilePerPage() throws Exception {
        Path pdfPath = createMultiPagePdf(tempDir, "multi.pdf", 3);
        String uploadId = "test-upload-002";

        ProcessingResult result = service.process(
                uploadId, "multi.pdf", uploadId + "_multi.pdf",
                pdfPath, Files.size(pdfPath), Instant.now());

        assertThat(result.getTotalPages()).isEqualTo(3);
        Path textDir = tempDir.resolve("processed/" + uploadId + "/text");
        assertThat(textDir.resolve("page-1.txt")).exists();
        assertThat(textDir.resolve("page-2.txt")).exists();
        assertThat(textDir.resolve("page-3.txt")).exists();
    }

    // ── Metadata generation ───────────────────────────────────────────────────

    @Test
    void metadataGeneration_writesCorrectFields() throws Exception {
        Path pdfPath = createPdfWithText(tempDir, "meta-test.pdf", "Metadata test content");
        String uploadId = "test-upload-003";
        Instant before  = Instant.now();

        service.process(uploadId, "meta-test.pdf", uploadId + "_meta-test.pdf",
                pdfPath, 12345L, before);

        File metaFile = tempDir.resolve("processed/" + uploadId + "/metadata.json").toFile();
        assertThat(metaFile).exists();

        DocumentMetadata meta = objectMapper.readValue(metaFile, DocumentMetadata.class);
        assertThat(meta.getUploadId()).isEqualTo(uploadId);
        assertThat(meta.getOriginalFileName()).isEqualTo("meta-test.pdf");
        assertThat(meta.getTotalPages()).isEqualTo(1);
        assertThat(meta.getFileSizeBytes()).isEqualTo(12345L);
        assertThat(meta.getProcessingDurationMs()).isGreaterThanOrEqualTo(0);
    }

    // ── Processing report generation ──────────────────────────────────────────

    @Test
    void processingReport_success_writesReportFile() throws Exception {
        Path pdfPath = createPdfWithText(tempDir, "report-test.pdf", "Report test");
        String uploadId = "test-upload-004";

        service.process(uploadId, "report-test.pdf", uploadId + "_report-test.pdf",
                pdfPath, Files.size(pdfPath), Instant.now());

        File reportFile = tempDir.resolve("processed/" + uploadId + "/processing-report.json").toFile();
        assertThat(reportFile).exists();

        ProcessingReport report = objectMapper.readValue(reportFile, ProcessingReport.class);
        assertThat(report.getStatus()).isEqualTo(ProcessingStatus.SUCCESS);
        assertThat(report.getPagesProcessed()).isEqualTo(1);
        assertThat(report.getCompletionTimestamp()).isNotNull();
    }

    // ── Non-PDF passthrough ───────────────────────────────────────────────────

    @Test
    void nonPdfFile_returnsSuccessWithoutProcessing() throws Exception {
        Path txtPath = tempDir.resolve("notes.txt");
        Files.writeString(txtPath, "Just plain text");
        String uploadId = "test-upload-005";

        ProcessingResult result = service.process(
                uploadId, "notes.txt", uploadId + "_notes.txt",
                txtPath, Files.size(txtPath), Instant.now());

        assertThat(result.getStatus()).isEqualTo(ProcessingStatus.SUCCESS);
        // No processed directory should be created for non-PDFs
        Path processedDir = tempDir.resolve("processed/" + uploadId);
        assertThat(processedDir).doesNotExist();
    }

    // ── Directory structure ───────────────────────────────────────────────────

    @Test
    void outputDirectories_areCreatedCorrectly() throws Exception {
        Path pdfPath = createPdfWithText(tempDir, "dirs.pdf", "Directory test");
        String uploadId = "test-upload-006";

        service.process(uploadId, "dirs.pdf", uploadId + "_dirs.pdf",
                pdfPath, Files.size(pdfPath), Instant.now());

        Path root = tempDir.resolve("processed/" + uploadId);
        assertThat(root.resolve("text")).isDirectory();
        assertThat(root.resolve("images")).isDirectory();
        assertThat(root.resolve("metadata.json")).exists();
        assertThat(root.resolve("processing-report.json")).exists();
    }

    // ── Image extraction (no images) ──────────────────────────────────────────

    @Test
    void imageExtraction_pdfWithoutImages_reportsZeroImages() throws Exception {
        Path pdfPath = createPdfWithText(tempDir, "no-images.pdf", "Text only, no images");
        String uploadId = "test-upload-007";

        ProcessingResult result = service.process(
                uploadId, "no-images.pdf", uploadId + "_no-images.pdf",
                pdfPath, Files.size(pdfPath), Instant.now());

        assertThat(result.getTotalImages()).isEqualTo(0);
        assertThat(result.getStatus()).isEqualTo(ProcessingStatus.SUCCESS);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Path createPdfWithText(Path dir, String filename, String text) throws IOException {
        Path pdfPath = dir.resolve(filename);
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }
            doc.save(pdfPath.toFile());
        }
        return pdfPath;
    }

    private Path createMultiPagePdf(Path dir, String filename, int pageCount) throws IOException {
        Path pdfPath = dir.resolve(filename);
        try (PDDocument doc = new PDDocument()) {
            for (int i = 1; i <= pageCount; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    cs.newLineAtOffset(50, 700);
                    cs.showText("Page " + i + " content");
                    cs.endText();
                }
            }
            doc.save(pdfPath.toFile());
        }
        return pdfPath;
    }
}
