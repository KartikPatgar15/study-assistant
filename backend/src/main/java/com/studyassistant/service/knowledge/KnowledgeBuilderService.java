package com.studyassistant.service.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyassistant.dto.knowledge.KnowledgeChunk;
import com.studyassistant.dto.knowledge.KnowledgeDocument;
import com.studyassistant.dto.knowledge.KnowledgeSummary;
import com.studyassistant.service.processing.ProcessedStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Orchestrates the Knowledge Builder pipeline (M03.5).
 *
 * <p><b>Pipeline:</b>
 * <ol>
 *   <li>Read per-page {@code .txt} files from {@code processed/<uploadId>/text/}.</li>
 *   <li>Delegate to {@link SemanticChunker} to produce raw semantic chunks.</li>
 *   <li>Delegate to {@link ImageAssociator} to bind images to chunks.</li>
 *   <li>Delegate to {@link KeywordExtractor} to produce per-chunk keywords.</li>
 *   <li>Delegate to {@link LanguageDetector} to detect document language.</li>
 *   <li>Assign sequential {@code chunkId}s and wire doubly-linked navigation.</li>
 *   <li>Serialise {@link KnowledgeDocument} to {@code processed/<uploadId>/knowledge.json}.</li>
 *   <li>Return {@link KnowledgeSummary} for embedding in the API response.</li>
 * </ol>
 *
 * <p><b>Single Responsibility:</b> orchestration only.
 * Text chunking, image association, keyword extraction, and language detection
 * each live in their own dedicated service.
 *
 * <p>All recoverable errors (missing page files, unreadable images) generate
 * warnings in the log and continue rather than aborting the build.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBuilderService {

    private final ProcessedStorage  processedStorage;
    private final SemanticChunker   semanticChunker;
    private final ImageAssociator   imageAssociator;
    private final KeywordExtractor  keywordExtractor;
    private final LanguageDetector  languageDetector;
    private final ObjectMapper      objectMapper;

    /** Page file naming convention written by M03: {@code page-N.txt}. */
    private static final Pattern PAGE_FILE_PATTERN = Pattern.compile("page-(\\d+)\\.txt");

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Build the knowledge representation for one upload.
     *
     * @param uploadId  the UUID assigned during upload
     * @return {@link KnowledgeSummary} for embedding in the API response;
     *         an empty summary is returned (not an exception) if the output
     *         directory has no page files
     * @throws IOException if {@code knowledge.json} cannot be written
     */
    public KnowledgeSummary build(String uploadId) throws IOException {
        Instant start = Instant.now();
        log.info("Knowledge build started – uploadId={}", uploadId);

        Path outputRoot = processedStorage.getOutputRoot(uploadId);
        Path textDir    = outputRoot.resolve("text");
        Path imagesDir  = outputRoot.resolve("images");

        // ── 1. Read page files ────────────────────────────────────────────
        log.info("Reading page files – uploadId={}", uploadId);
        List<SemanticChunker.PageText> pages = readPageFiles(textDir, uploadId);

        if (pages.isEmpty()) {
            log.warn("No page files found for uploadId={} – writing empty knowledge document", uploadId);
            return writeEmptyDocument(uploadId, outputRoot, start);
        }

        // Collect full text for language detection
        String fullText = pages.stream()
                .map(SemanticChunker.PageText::text)
                .collect(Collectors.joining(" "));

        // ── 2. Detect language ────────────────────────────────────────────
        String language = languageDetector.detect(fullText);
        log.debug("Detected language='{}' for uploadId={}", language, uploadId);

        // ── 3. Chunk ──────────────────────────────────────────────────────
        log.info("Generating chunks – uploadId={}", uploadId);
        List<SemanticChunker.RawChunk> rawChunks = semanticChunker.chunk(pages);

        if (rawChunks.isEmpty()) {
            log.warn("Chunking produced 0 chunks for uploadId={}", uploadId);
            return writeEmptyDocument(uploadId, outputRoot, start);
        }

        // ── 4. Associate images ───────────────────────────────────────────
        log.info("Associating images – uploadId={}", uploadId);
        List<List<Integer>> chunkPageLists = rawChunks.stream()
                .map(SemanticChunker.RawChunk::pages)
                .collect(Collectors.toList());
        Map<Integer, List<String>> imagesByChunk = imageAssociator.associate(imagesDir, chunkPageLists);

        // ── 5. Build KnowledgeChunk list ──────────────────────────────────
        log.info("Building knowledge base – uploadId={}", uploadId);
        Instant chunkTimestamp = Instant.now();
        List<KnowledgeChunk> chunks = new ArrayList<>();

        for (int i = 0; i < rawChunks.size(); i++) {
            SemanticChunker.RawChunk raw     = rawChunks.get(i);
            String                   text    = raw.textBuffer().toString().strip();
            List<Integer>            pgNums  = sortedUnique(raw.pages());
            int                      startPg = pgNums.isEmpty() ? 0 : pgNums.get(0);
            int                      endPg   = pgNums.isEmpty() ? 0 : pgNums.get(pgNums.size() - 1);
            List<String>             words   = wordList(text);
            List<String>             images  = imagesByChunk.getOrDefault(i, List.of());
            List<String>             keywords = keywordExtractor.extract(text);

            KnowledgeChunk chunk = KnowledgeChunk.builder()
                    .chunkId(i + 1)                // sequential 1-based integer
                    .chunkTitle(raw.heading())
                    .pages(pgNums)
                    .startPage(startPg)
                    .endPage(endPg)
                    .text(text)
                    .wordCount(words.size())
                    .characterCount(text.length())
                    .keywords(keywords)
                    .relatedImages(images)
                    .previousChunkId(i > 0 ? i : null)                          // chunkId of prev
                    .nextChunkId(i < rawChunks.size() - 1 ? i + 2 : null)       // chunkId of next
                    .sectionNumber(raw.sectionNumber())
                    .relevanceScore(0.0)
                    .createdTimestamp(chunkTimestamp)
                    .build();

            chunks.add(chunk);
        }

        // ── 6. Compute summary statistics ─────────────────────────────────
        int   totalImages   = chunks.stream().mapToInt(c -> c.getRelatedImages().size()).sum();
        int   totalWords    = chunks.stream().mapToInt(KnowledgeChunk::getWordCount).sum();
        int   largestWC     = chunks.stream().mapToInt(KnowledgeChunk::getWordCount).max().orElse(0);
        int   smallestWC    = chunks.stream().mapToInt(KnowledgeChunk::getWordCount).min().orElse(0);
        int   avgWC         = chunks.isEmpty() ? 0 : totalWords / chunks.size();
        int   pagesCovered  = (int) chunks.stream()
                .flatMap(c -> c.getPages().stream()).distinct().count();
        int   largestSpan   = chunks.stream()
                .mapToInt(c -> c.getEndPage() - c.getStartPage() + 1).max().orElse(0);
        long  durationMs    = Instant.now().toEpochMilli() - start.toEpochMilli();

        KnowledgeSummary summary = KnowledgeSummary.builder()
                .totalChunks(chunks.size())
                .averageChunkSize(avgWC)
                .largestChunk(largestWC)
                .smallestChunk(smallestWC)
                .pagesCovered(pagesCovered)
                .imagesLinked(totalImages)
                .largestPageSpan(largestSpan)
                .buildDurationMs(durationMs)
                .build();

        // ── 7. Write knowledge.json ───────────────────────────────────────
        log.info("Writing knowledge.json – uploadId={}", uploadId);
        KnowledgeDocument doc = KnowledgeDocument.builder()
                .uploadId(uploadId)
                .language(language)
                .createdTimestamp(start)
                .chunks(chunks)
                .summary(summary)
                .build();

        Path knowledgePath = outputRoot.resolve("knowledge.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(knowledgePath.toFile(), doc);

        log.info("Knowledge build completed – uploadId={}, chunks={}, pages={}, images={}, duration={}ms",
                uploadId, chunks.size(), pagesCovered, totalImages, durationMs);

        return summary;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Reads all {@code page-N.txt} files from {@code textDir}, sorted by page number.
     * Missing or unreadable files generate a warning and are skipped.
     */
    private List<SemanticChunker.PageText> readPageFiles(Path textDir, String uploadId) {
        if (!Files.isDirectory(textDir)) {
            log.warn("Text directory does not exist for uploadId={}: {}", uploadId, textDir);
            return List.of();
        }

        List<Path> pageFiles;
        try (Stream<Path> stream = Files.list(textDir)) {
            pageFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> PAGE_FILE_PATTERN.matcher(p.getFileName().toString()).matches())
                    .sorted(Comparator.comparingInt(p -> {
                        var m = PAGE_FILE_PATTERN.matcher(p.getFileName().toString());
                        return m.matches() ? Integer.parseInt(m.group(1)) : 0;
                    }))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.warn("Could not list text directory for uploadId={}: {}", uploadId, e.getMessage());
            return List.of();
        }

        List<SemanticChunker.PageText> result = new ArrayList<>();
        for (Path file : pageFiles) {
            var m = PAGE_FILE_PATTERN.matcher(file.getFileName().toString());
            if (!m.matches()) continue;
            int pageNum = Integer.parseInt(m.group(1));
            try {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                result.add(new SemanticChunker.PageText(pageNum, text));
            } catch (IOException e) {
                log.warn("Could not read page file {} for uploadId={}: {}",
                        file.getFileName(), uploadId, e.getMessage());
            }
        }

        log.debug("Read {} page files for uploadId={}", result.size(), uploadId);
        return result;
    }

    /** Returns a sorted list of unique page numbers. */
    private List<Integer> sortedUnique(List<Integer> pages) {
        return pages.stream().distinct().sorted().collect(Collectors.toList());
    }

    /** Splits text into individual words. */
    private List<String> wordList(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.asList(text.trim().split("\\s+"));
    }

    /** Writes a minimal empty {@link KnowledgeDocument} and returns an empty summary. */
    private KnowledgeSummary writeEmptyDocument(String uploadId, Path outputRoot,
                                                 Instant start) throws IOException {
        long durationMs = Instant.now().toEpochMilli() - start.toEpochMilli();
        KnowledgeSummary summary = KnowledgeSummary.builder().buildDurationMs(durationMs).build();
        KnowledgeDocument doc = KnowledgeDocument.builder()
                .uploadId(uploadId)
                .createdTimestamp(start)
                .chunks(List.of())
                .summary(summary)
                .build();
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(outputRoot.resolve("knowledge.json").toFile(), doc);
        return summary;
    }
}
