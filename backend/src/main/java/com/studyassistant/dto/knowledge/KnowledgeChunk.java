package com.studyassistant.dto.knowledge;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * A single semantic unit of knowledge extracted from the document.
 *
 * <p>Each chunk represents a coherent block of text — a heading plus its
 * supporting paragraphs, or a multi-page section that belongs to one topic.
 * Chunks are doubly-linked via {@link #previousChunkId} / {@link #nextChunkId}
 * so M04 can expand its context window in either direction.
 *
 * <p>Fields annotated {@code @JsonInclude(NON_NULL)} are omitted when absent
 * so the JSON stays compact for chunks without headings or section numbers.
 *
 * <p>Design for extension: add new fields to the Builder without touching
 * existing consumers — Jackson ignores unknown fields by default.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeChunk {

    /** Sequential 1-based integer. Stable within a document build run. */
    private final int          chunkId;

    /** Detected heading text, or null if the chunk has no heading. */
    private final String       chunkTitle;

    /** Every page number that contributed text to this chunk. */
    private final List<Integer> pages;

    /** First page number in {@link #pages}. Convenience field for range queries. */
    private final int          startPage;

    /** Last page number in {@link #pages}. Convenience field for range queries. */
    private final int          endPage;

    /** The full extracted and cleaned text of this chunk. */
    private final String       text;

    private final int          wordCount;
    private final int          characterCount;

    /**
     * Top-N content keywords extracted via frequency analysis after stop-word
     * removal. Used by M04 for lightweight keyword-based retrieval before
     * sending chunks to an AI provider.
     */
    private final List<String> keywords;

    /** Filenames (not paths) of images whose page falls within this chunk's page range. */
    private final List<String> relatedImages;

    private final Integer      previousChunkId;
    private final Integer      nextChunkId;

    /**
     * Detected section number (e.g. "3.2") parsed from the heading, or null.
     * Null is omitted from JSON output.
     */
    private final String       sectionNumber;

    /**
     * Reserved for future AI-based relevance scoring.
     * Defaults to 0 — renamed from {@code confidenceScore} per M03.5 spec
     * because this module is about knowledge representation, not AI confidence.
     */
    private final double       relevanceScore;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final Instant      createdTimestamp;

    private KnowledgeChunk(Builder b) {
        this.chunkId          = b.chunkId;
        this.chunkTitle       = b.chunkTitle;
        this.pages            = List.copyOf(b.pages);
        this.startPage        = b.startPage;
        this.endPage          = b.endPage;
        this.text             = b.text;
        this.wordCount        = b.wordCount;
        this.characterCount   = b.characterCount;
        this.keywords         = List.copyOf(b.keywords);
        this.relatedImages    = List.copyOf(b.relatedImages);
        this.previousChunkId  = b.previousChunkId;
        this.nextChunkId      = b.nextChunkId;
        this.sectionNumber    = b.sectionNumber;
        this.relevanceScore   = b.relevanceScore;
        this.createdTimestamp = b.createdTimestamp;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int           getChunkId()          { return chunkId; }
    public String        getChunkTitle()        { return chunkTitle; }
    public List<Integer> getPages()             { return pages; }
    public int           getStartPage()         { return startPage; }
    public int           getEndPage()           { return endPage; }
    public String        getText()              { return text; }
    public int           getWordCount()         { return wordCount; }
    public int           getCharacterCount()    { return characterCount; }
    public List<String>  getKeywords()          { return keywords; }
    public List<String>  getRelatedImages()     { return relatedImages; }
    public Integer       getPreviousChunkId()   { return previousChunkId; }
    public Integer       getNextChunkId()       { return nextChunkId; }
    public String        getSectionNumber()     { return sectionNumber; }
    public double        getRelevanceScore()    { return relevanceScore; }
    public Instant       getCreatedTimestamp()  { return createdTimestamp; }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int          chunkId          = 0;
        private String       chunkTitle       = null;
        private List<Integer> pages           = List.of();
        private int          startPage        = 0;
        private int          endPage          = 0;
        private String       text             = "";
        private int          wordCount        = 0;
        private int          characterCount   = 0;
        private List<String> keywords         = List.of();
        private List<String> relatedImages    = List.of();
        private Integer      previousChunkId  = null;
        private Integer      nextChunkId      = null;
        private String       sectionNumber    = null;
        private double       relevanceScore   = 0.0;
        private Instant      createdTimestamp = Instant.now();

        public Builder chunkId(int v)               { chunkId = v;          return this; }
        public Builder chunkTitle(String v)         { chunkTitle = v;       return this; }
        public Builder pages(List<Integer> v)       { pages = v;            return this; }
        public Builder startPage(int v)             { startPage = v;        return this; }
        public Builder endPage(int v)               { endPage = v;          return this; }
        public Builder text(String v)               { text = v;             return this; }
        public Builder wordCount(int v)             { wordCount = v;        return this; }
        public Builder characterCount(int v)        { characterCount = v;   return this; }
        public Builder keywords(List<String> v)     { keywords = v;         return this; }
        public Builder relatedImages(List<String> v){ relatedImages = v;    return this; }
        public Builder previousChunkId(Integer v)   { previousChunkId = v;  return this; }
        public Builder nextChunkId(Integer v)       { nextChunkId = v;      return this; }
        public Builder sectionNumber(String v)      { sectionNumber = v;    return this; }
        public Builder relevanceScore(double v)     { relevanceScore = v;   return this; }
        public Builder createdTimestamp(Instant v)  { createdTimestamp = v; return this; }

        public KnowledgeChunk build() { return new KnowledgeChunk(this); }
    }
}
