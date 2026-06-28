package com.studyassistant.dto.knowledge;

/**
 * Aggregate statistics for one knowledge build run.
 *
 * <p>Embedded in both the {@code POST /api/upload} response (so the frontend
 * can render it immediately) and in {@code knowledge.json} (so M04 can read
 * it without re-scanning all chunks).
 *
 * <p>Immutable – constructed via {@link Builder}.
 */
public class KnowledgeSummary {

    private final int  totalChunks;
    private final int  averageChunkSize;   // average word count per chunk
    private final int  largestChunk;       // word count of the largest chunk
    private final int  smallestChunk;      // word count of the smallest chunk
    private final int  pagesCovered;       // distinct pages that contributed text
    private final int  imagesLinked;       // total image references across all chunks
    private final int  largestPageSpan;    // max number of pages spanned by a single chunk
    private final long buildDurationMs;

    private KnowledgeSummary(Builder b) {
        this.totalChunks      = b.totalChunks;
        this.averageChunkSize = b.averageChunkSize;
        this.largestChunk     = b.largestChunk;
        this.smallestChunk    = b.smallestChunk;
        this.pagesCovered     = b.pagesCovered;
        this.imagesLinked     = b.imagesLinked;
        this.largestPageSpan  = b.largestPageSpan;
        this.buildDurationMs  = b.buildDurationMs;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int  getTotalChunks()      { return totalChunks; }
    public int  getAverageChunkSize() { return averageChunkSize; }
    public int  getLargestChunk()     { return largestChunk; }
    public int  getSmallestChunk()    { return smallestChunk; }
    public int  getPagesCovered()     { return pagesCovered; }
    public int  getImagesLinked()     { return imagesLinked; }
    public int  getLargestPageSpan()  { return largestPageSpan; }
    public long getBuildDurationMs()  { return buildDurationMs; }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int  totalChunks      = 0;
        private int  averageChunkSize = 0;
        private int  largestChunk     = 0;
        private int  smallestChunk    = 0;
        private int  pagesCovered     = 0;
        private int  imagesLinked     = 0;
        private int  largestPageSpan  = 0;
        private long buildDurationMs  = 0;

        public Builder totalChunks(int v)      { totalChunks = v;      return this; }
        public Builder averageChunkSize(int v)  { averageChunkSize = v; return this; }
        public Builder largestChunk(int v)      { largestChunk = v;     return this; }
        public Builder smallestChunk(int v)     { smallestChunk = v;    return this; }
        public Builder pagesCovered(int v)      { pagesCovered = v;     return this; }
        public Builder imagesLinked(int v)      { imagesLinked = v;     return this; }
        public Builder largestPageSpan(int v)   { largestPageSpan = v;  return this; }
        public Builder buildDurationMs(long v)  { buildDurationMs = v;  return this; }

        public KnowledgeSummary build() { return new KnowledgeSummary(this); }
    }
}
