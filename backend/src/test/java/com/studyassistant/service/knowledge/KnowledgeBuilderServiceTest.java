package com.studyassistant.service.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.studyassistant.dto.knowledge.KnowledgeDocument;
import com.studyassistant.dto.knowledge.KnowledgeSummary;
import com.studyassistant.service.processing.ProcessedStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit and integration tests for the Knowledge Builder pipeline.
 *
 * <p>All tests use {@code @TempDir} – no real filesystem state is left behind.
 * The full service graph is wired manually (no Spring context) so tests are fast.
 */
class KnowledgeBuilderServiceTest {

    @TempDir
    Path tempDir;

    private KnowledgeBuilderService service;
    private ObjectMapper            objectMapper;
    private ProcessedStorage        storage;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        storage = new ProcessedStorage();
        ReflectionTestUtils.setField(storage, "outputDirectory",
                tempDir.resolve("processed").toString());
        storage.init();

        service = new KnowledgeBuilderService(
                storage,
                new SemanticChunker(),
                new ImageAssociator(),
                new KeywordExtractor(),
                new LanguageDetector(),
                objectMapper
        );
    }

    // ── Missing text directory ────────────────────────────────────────────────

    @Test
    void build_missingTextDirectory_returnsEmptySummaryAndWritesFile() throws Exception {
        String uploadId = "no-text-dir";
        storage.createOutputDirectories(uploadId);
        // Do NOT create text/ – simulate a non-PDF upload

        KnowledgeSummary summary = service.build(uploadId);

        assertThat(summary.getTotalChunks()).isEqualTo(0);
        Path knowledgeFile = tempDir.resolve("processed/" + uploadId + "/knowledge.json");
        assertThat(knowledgeFile).exists();
    }

    // ── Single-page document ──────────────────────────────────────────────────

    @Test
    void build_singlePage_producesAtLeastOneChunk() throws Exception {
        String uploadId = "single-page";
        Path root = storage.createOutputDirectories(uploadId);
        writePageFile(root, 1, "Introduction to Operating Systems\n\n"
                + "An operating system is system software that manages computer hardware, "
                + "software resources, and provides common services for computer programs. "
                + "Time-sharing operating systems schedule tasks for efficient use of the system "
                + "and may also include accounting software for cost allocation of processor time, "
                + "mass storage, printing, and other resources.");

        KnowledgeSummary summary = service.build(uploadId);

        assertThat(summary.getTotalChunks()).isGreaterThanOrEqualTo(1);
        assertThat(summary.getPagesCovered()).isEqualTo(1);
        assertThat(summary.getBuildDurationMs()).isGreaterThanOrEqualTo(0);
    }

    // ── knowledge.json written correctly ─────────────────────────────────────

    @Test
    void build_writesKnowledgeJsonWithCorrectStructure() throws Exception {
        String uploadId = "structure-test";
        Path root = storage.createOutputDirectories(uploadId);
        writePageFile(root, 1, "Memory Management\n\n"
                + "Memory management is the process of controlling and coordinating computer memory, "
                + "assigning portions called blocks to various running programs to optimize overall "
                + "system performance. Memory management is part of the operating system functionality.");

        service.build(uploadId);

        Path knowledgePath = tempDir.resolve("processed/" + uploadId + "/knowledge.json");
        assertThat(knowledgePath).exists();

        KnowledgeDocument doc = objectMapper.readValue(knowledgePath.toFile(), KnowledgeDocument.class);
        assertThat(doc.getUploadId()).isEqualTo(uploadId);
        assertThat(doc.getChunks()).isNotEmpty();
        assertThat(doc.getSummary()).isNotNull();
        assertThat(doc.getCreatedTimestamp()).isNotNull();
    }

    // ── Sequential chunkId values ─────────────────────────────────────────────

    @Test
    void build_chunkIds_areSequentialIntegers() throws Exception {
        String uploadId = "chunk-ids";
        Path root = storage.createOutputDirectories(uploadId);
        // Two pages of content large enough to produce multiple chunks
        writePageFile(root, 1, largePageText("Process Scheduling", "scheduling", 80));
        writePageFile(root, 2, largePageText("Memory Allocation", "memory", 80));

        service.build(uploadId);

        KnowledgeDocument doc = objectMapper.readValue(
                tempDir.resolve("processed/" + uploadId + "/knowledge.json").toFile(),
                KnowledgeDocument.class);

        for (int i = 0; i < doc.getChunks().size(); i++) {
            assertThat(doc.getChunks().get(i).getChunkId()).isEqualTo(i + 1);
        }
    }

    // ── startPage / endPage ───────────────────────────────────────────────────

    @Test
    void build_chunks_haveCorrectStartAndEndPage() throws Exception {
        String uploadId = "page-range";
        Path root = storage.createOutputDirectories(uploadId);
        writePageFile(root, 3, largePageText("Virtual Memory", "virtual", 60));

        service.build(uploadId);

        KnowledgeDocument doc = objectMapper.readValue(
                tempDir.resolve("processed/" + uploadId + "/knowledge.json").toFile(),
                KnowledgeDocument.class);

        doc.getChunks().forEach(chunk -> {
            assertThat(chunk.getStartPage()).isGreaterThan(0);
            assertThat(chunk.getEndPage()).isGreaterThanOrEqualTo(chunk.getStartPage());
            assertThat(chunk.getPages()).contains(chunk.getStartPage());
            assertThat(chunk.getPages()).contains(chunk.getEndPage());
        });
    }

    // ── Keywords ─────────────────────────────────────────────────────────────

    @Test
    void build_chunks_haveKeywords() throws Exception {
        String uploadId = "keywords-test";
        Path root = storage.createOutputDirectories(uploadId);
        writePageFile(root, 1, largePageText("Deadlock Detection", "deadlock", 60));

        service.build(uploadId);

        KnowledgeDocument doc = objectMapper.readValue(
                tempDir.resolve("processed/" + uploadId + "/knowledge.json").toFile(),
                KnowledgeDocument.class);

        doc.getChunks().forEach(chunk ->
                assertThat(chunk.getKeywords()).isNotNull());
    }

    // ── Language detection ────────────────────────────────────────────────────

    @Test
    void build_englishDocument_detectsEnglishLanguage() throws Exception {
        String uploadId = "language-test";
        Path root = storage.createOutputDirectories(uploadId);
        writePageFile(root, 1, largePageText("File Systems", "file", 60));

        service.build(uploadId);

        KnowledgeDocument doc = objectMapper.readValue(
                tempDir.resolve("processed/" + uploadId + "/knowledge.json").toFile(),
                KnowledgeDocument.class);

        assertThat(doc.getLanguage()).isEqualTo("en");
    }

    // ── Summary statistics ────────────────────────────────────────────────────

    @Test
    void build_summary_containsAllRequiredFields() throws Exception {
        String uploadId = "summary-stats";
        Path root = storage.createOutputDirectories(uploadId);
        writePageFile(root, 1, largePageText("I/O Systems", "input output", 80));
        writePageFile(root, 2, largePageText("Disk Scheduling", "disk", 80));

        KnowledgeSummary summary = service.build(uploadId);

        assertThat(summary.getTotalChunks()).isGreaterThanOrEqualTo(1);
        assertThat(summary.getLargestChunk()).isGreaterThan(0);
        assertThat(summary.getSmallestChunk()).isGreaterThan(0);
        assertThat(summary.getSmallestChunk()).isLessThanOrEqualTo(summary.getLargestChunk());
        assertThat(summary.getPagesCovered()).isGreaterThan(0);
        assertThat(summary.getLargestPageSpan()).isGreaterThan(0);
        assertThat(summary.getBuildDurationMs()).isGreaterThanOrEqualTo(0);
    }

    // ── Image association ─────────────────────────────────────────────────────

    @Test
    void build_withImages_linksImagesToChunks() throws Exception {
        String uploadId = "image-assoc";
        Path root = storage.createOutputDirectories(uploadId);
        writePageFile(root, 1, largePageText("CPU Architecture", "cpu processor", 60));

        // Write a fake image file matching M03 naming convention
        Path imagesDir = root.resolve("images");
        Files.writeString(imagesDir.resolve("page-1-img-1.png"), "fake-image-data");

        service.build(uploadId);

        KnowledgeDocument doc = objectMapper.readValue(
                tempDir.resolve("processed/" + uploadId + "/knowledge.json").toFile(),
                KnowledgeDocument.class);

        long totalImageRefs = doc.getChunks().stream()
                .mapToLong(c -> c.getRelatedImages().size()).sum();
        assertThat(totalImageRefs).isGreaterThan(0);
        assertThat(doc.getSummary().getImagesLinked()).isGreaterThan(0);
    }

    // ── Doubly-linked chunk navigation ───────────────────────────────────────

    @Test
    void build_chunks_areCorrectlyLinked() throws Exception {
        String uploadId = "linked-chunks";
        Path root = storage.createOutputDirectories(uploadId);
        writePageFile(root, 1, largePageText("Synchronization", "mutex lock", 60));
        writePageFile(root, 2, largePageText("Semaphores", "semaphore signal", 60));

        service.build(uploadId);

        KnowledgeDocument doc = objectMapper.readValue(
                tempDir.resolve("processed/" + uploadId + "/knowledge.json").toFile(),
                KnowledgeDocument.class);

        var chunks = doc.getChunks();
        if (chunks.size() >= 2) {
            // First chunk has no previous
            assertThat(chunks.get(0).getPreviousChunkId()).isNull();
            // Last chunk has no next
            assertThat(chunks.get(chunks.size() - 1).getNextChunkId()).isNull();
            // Middle links are consistent
            for (int i = 1; i < chunks.size() - 1; i++) {
                assertThat(chunks.get(i).getPreviousChunkId()).isNotNull();
                assertThat(chunks.get(i).getNextChunkId()).isNotNull();
            }
        }
    }

    // ── SemanticChunker unit tests ────────────────────────────────────────────

    @Test
    void semanticChunker_headingDetection_recognisesAllCaps() {
        SemanticChunker chunker = new SemanticChunker();
        assertThat(chunker.isHeading("INTRODUCTION", "")).isTrue();
        assertThat(chunker.isHeading("MEMORY MANAGEMENT", "")).isTrue();
    }

    @Test
    void semanticChunker_headingDetection_rejectsSentences() {
        SemanticChunker chunker = new SemanticChunker();
        assertThat(chunker.isHeading("This is a full sentence that ends with a period.", "")).isFalse();
        assertThat(chunker.isHeading("Another sentence, with a comma.", "")).isFalse();
    }

    @Test
    void semanticChunker_headingDetection_recognisesSectionNumbers() {
        SemanticChunker chunker = new SemanticChunker();
        assertThat(chunker.isHeading("3.2 Scheduling Algorithms", "")).isTrue();
        assertThat(chunker.isHeading("12.4.1 Round Robin Scheduling", "")).isTrue();
    }

    // ── KeywordExtractor unit tests ───────────────────────────────────────────

    @Test
    void keywordExtractor_returnsTopKeywords() {
        KeywordExtractor extractor = new KeywordExtractor();
        String text = "process process process scheduling scheduling cpu cpu cpu cpu memory";
        List<String> keywords = extractor.extract(text, 3);
        assertThat(keywords).contains("cpu", "process");
        assertThat(keywords).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    void keywordExtractor_removesStopWords() {
        KeywordExtractor extractor = new KeywordExtractor();
        String text = "the and is of in it this that are was were for to";
        List<String> keywords = extractor.extract(text);
        assertThat(keywords).isEmpty();
    }

    // ── LanguageDetector unit tests ───────────────────────────────────────────

    @Test
    void languageDetector_detectsEnglish() {
        LanguageDetector detector = new LanguageDetector();
        String text = "the and of to in is it that this with for are as was at be by from or an "
                    + "the operating system manages computer hardware and software resources "
                    + "the process scheduler allocates cpu time to running processes";
        assertThat(detector.detect(text)).isEqualTo("en");
    }

    @Test
    void languageDetector_shortText_returnsDefault() {
        LanguageDetector detector = new LanguageDetector();
        assertThat(detector.detect("Hello world")).isEqualTo("en");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void writePageFile(Path root, int pageNum, String content) throws Exception {
        Path textDir = root.resolve("text");
        Files.createDirectories(textDir);
        Files.writeString(textDir.resolve("page-" + pageNum + ".txt"),
                content, StandardCharsets.UTF_8);
    }

    /** Generates a realistic paragraph of repeated domain text ≥ minWords words. */
    private String largePageText(String heading, String topic, int minWords) {
        StringBuilder sb = new StringBuilder(heading).append("\n\n");
        String sentence = "The " + topic + " system provides efficient management of resources "
                + "in modern computing environments. ";
        while (sb.toString().split("\\s+").length < minWords) {
            sb.append(sentence);
        }
        return sb.toString();
    }
}
