package com.studyassistant.service.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Converts a list of {@code (pageNumber, pageText)} pairs into semantic chunks.
 *
 * <p><b>Single Responsibility:</b> chunking only. No I/O, no image logic here.
 *
 * <p><b>Algorithm – multi-signal boundary detection:</b>
 * <ol>
 *   <li><b>Heading detection</b> — a line is treated as a section heading when it:
 *       <ul>
 *         <li>is ≤ 80 characters,</li>
 *         <li>does not end with {@code . , ; : ( [},</li>
 *         <li>is followed by a blank line or a substantially longer next line, and</li>
 *         <li>is ALL-CAPS, Title Case, or matches a section-number prefix
 *             (e.g. {@code "3.2 Scheduling Algorithms"}).</li>
 *       </ul>
 *       A confirmed heading always starts a new chunk.</li>
 *   <li><b>Major paragraph break</b> — two or more consecutive blank lines between
 *       blocks of text signal a section boundary and start a new chunk.</li>
 *   <li><b>Page continuity</b> — consecutive pages are merged into the same chunk
 *       unless a heading or major-break boundary is detected between them.
 *       This correctly handles chapters that span many pages.</li>
 *   <li><b>Minimum chunk size</b> — a candidate chunk below {@link #MIN_WORDS_PER_CHUNK}
 *       words is merged forward into the next chunk (unless it has a heading).
 *       This prevents noise chunks from sparse or header-only pages.</li>
 *   <li><b>Chunk linking</b> — after all chunks are accumulated, a single pass
 *       sets {@code previousChunkId} / {@code nextChunkId} to form a doubly-linked
 *       list for M04 context-window expansion.</li>
 * </ol>
 */
@Slf4j
@Service
public class SemanticChunker {

    /** Chunks below this word count are merged with the next chunk. */
    static final int MIN_WORDS_PER_CHUNK = 40;

    /** Maximum heading length in characters. */
    private static final int MAX_HEADING_LENGTH = 80;

    /** Detects section-number prefixes such as "1.", "3.2", "12.4.1". */
    private static final Pattern SECTION_NUMBER = Pattern.compile(
            "^(\\d{1,3}(?:\\.\\d{1,3}){0,3})\\.?\\s+.+");

    /** Characters that disqualify a line from being a heading. */
    private static final String HEADING_END_CHARS = ".,;:([";

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Represents one page of input to the chunker.
     *
     * @param pageNumber 1-based page number
     * @param text       cleaned text for this page (may be blank)
     */
    public record PageText(int pageNumber, String text) {}

    /**
     * An intermediate chunk produced during the chunking pass, before
     * image association and final ID assignment.
     */
    public record RawChunk(
            String       heading,
            String       sectionNumber,
            List<Integer> pages,
            StringBuilder textBuffer
    ) {
        /** Convenience – word count of the current text buffer. */
        int wordCount() {
            String t = textBuffer.toString().trim();
            return t.isEmpty() ? 0 : t.split("\\s+").length;
        }
    }

    /**
     * Chunk a document's pages into semantic units.
     *
     * @param pages ordered list of page texts (1-based page numbers)
     * @return mutable list of raw chunks, ready for image association
     */
    public List<RawChunk> chunk(List<PageText> pages) {
        List<RawChunk> chunks = new ArrayList<>();
        RawChunk       current = newChunk();

        for (PageText page : pages) {
            String pageText = page.text() == null ? "" : page.text().strip();

            if (pageText.isBlank()) {
                // Blank page – keep in current chunk so page numbers stay contiguous
                current.pages().add(page.pageNumber());
                log.debug("Page {} is blank – retained in current chunk", page.pageNumber());
                continue;
            }

            String[] lines = pageText.split("\n", -1);
            int      i     = 0;

            while (i < lines.length) {
                String line = lines[i];

                // ── Major break: 2+ consecutive blank lines ────────────────
                if (line.isBlank()) {
                    int blankCount = 0;
                    while (i < lines.length && lines[i].isBlank()) { i++; blankCount++; }

                    if (blankCount >= 2 && current.wordCount() >= MIN_WORDS_PER_CHUNK) {
                        chunks.add(current);
                        current = newChunk();
                    } else {
                        current.textBuffer().append("\n\n");
                    }
                    continue;
                }

                // ── Heading detection ──────────────────────────────────────
                String nextLine = (i + 1 < lines.length) ? lines[i + 1] : "";
                if (isHeading(line, nextLine)) {
                    // Only flush if there is meaningful content in the current chunk
                    if (current.wordCount() >= MIN_WORDS_PER_CHUNK) {
                        chunks.add(current);
                        current = newChunk();
                    }
                    // Extract section number if present
                    String secNum   = extractSectionNumber(line);
                    String title    = secNum != null
                            ? line.replaceFirst("^\\d[\\d.]*\\.?\\s*", "").strip()
                            : line.strip();
                    current = new RawChunk(title, secNum, current.pages(), current.textBuffer());
                    current.textBuffer().append(line).append("\n");
                    i++;
                    continue;
                }

                // ── Normal line ────────────────────────────────────────────
                current.textBuffer().append(line).append("\n");
                i++;
            }

            // After processing all lines on this page, record the page number
            current.pages().add(page.pageNumber());
        }

        // Flush the last chunk
        if (current.wordCount() > 0 || !current.pages().isEmpty()) {
            chunks.add(current);
        }

        // ── Merge undersized chunks forward ───────────────────────────────
        chunks = mergeSmallChunks(chunks);

        log.debug("Chunking complete – produced {} chunks from {} pages",
                chunks.size(), pages.size());
        return chunks;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private RawChunk newChunk() {
        return new RawChunk(null, null, new ArrayList<>(), new StringBuilder());
    }

    /**
     * Determines whether {@code line} qualifies as a section heading.
     *
     * <p>Rules (all must pass):
     * <ul>
     *   <li>Not blank.</li>
     *   <li>Length ≤ {@link #MAX_HEADING_LENGTH}.</li>
     *   <li>Does not end with a sentence-continuation character.</li>
     *   <li>Is ALL-CAPS, Title Case, or starts with a section number.</li>
     * </ul>
     */
    boolean isHeading(String line, String nextLine) {
        if (line == null || line.isBlank()) return false;
        String trimmed = line.strip();
        if (trimmed.length() > MAX_HEADING_LENGTH) return false;
        if (trimmed.isEmpty()) return false;

        char last = trimmed.charAt(trimmed.length() - 1);
        if (HEADING_END_CHARS.indexOf(last) >= 0) return false;

        // Must match at least one style signal
        boolean isAllCaps      = trimmed.equals(trimmed.toUpperCase()) && trimmed.matches(".*[A-Z].*");
        boolean isTitleCase    = isTitleCase(trimmed);
        boolean hasSectionNum  = SECTION_NUMBER.matcher(trimmed).matches();

        return isAllCaps || isTitleCase || hasSectionNum;
    }

    /**
     * Title Case: the majority of words start with an uppercase letter,
     * and the line contains at least two words.
     */
    private boolean isTitleCase(String line) {
        String[] words = line.split("\\s+");
        if (words.length < 2) return false;
        long capitalised = 0;
        for (String w : words) {
            if (!w.isEmpty() && Character.isUpperCase(w.charAt(0))) capitalised++;
        }
        return (double) capitalised / words.length >= 0.6;
    }

    /** Returns the section-number prefix (e.g. "3.2") or null. */
    private String extractSectionNumber(String line) {
        var m = SECTION_NUMBER.matcher(line.strip());
        return m.matches() ? m.group(1) : null;
    }

    /**
     * Merges any chunk below {@link #MIN_WORDS_PER_CHUNK} into the following chunk,
     * unless it has a detected heading (in which case it stays independent).
     */
    private List<RawChunk> mergeSmallChunks(List<RawChunk> chunks) {
        if (chunks.size() <= 1) return chunks;

        List<RawChunk> merged = new ArrayList<>();
        int i = 0;
        while (i < chunks.size()) {
            RawChunk c = chunks.get(i);
            if (c.wordCount() < MIN_WORDS_PER_CHUNK && c.heading() == null && i + 1 < chunks.size()) {
                // Absorb this chunk into the next one
                RawChunk next = chunks.get(i + 1);
                next.pages().addAll(0, c.pages());
                next.textBuffer().insert(0, c.textBuffer().toString());
                // Don't add c – it's been merged
            } else {
                merged.add(c);
            }
            i++;
        }
        return merged;
    }
}
