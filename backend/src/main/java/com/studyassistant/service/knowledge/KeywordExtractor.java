package com.studyassistant.service.knowledge;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Extracts the top-N content keywords from a text using term-frequency analysis
 * after stop-word removal.
 *
 * <p><b>Single Responsibility:</b> keyword extraction only. No AI, no embeddings.
 *
 * <p><b>Algorithm:</b>
 * <ol>
 *   <li>Tokenise on whitespace and punctuation.</li>
 *   <li>Lower-case all tokens.</li>
 *   <li>Discard tokens that are in the stop-word set, shorter than 3 characters,
 *       or purely numeric.</li>
 *   <li>Count term frequency across the remaining tokens.</li>
 *   <li>Return the top {@code maxKeywords} tokens sorted by frequency descending,
 *       then alphabetically to make results deterministic.</li>
 * </ol>
 */
@Service
public class KeywordExtractor {

    public static final int DEFAULT_MAX_KEYWORDS = 10;

    /**
     * Common English stop words.
     * Stored in a HashSet (not Set.of) to guarantee no duplicate-element errors
     * at class initialisation time.
     */
    private static final Set<String> STOP_WORDS;

    static {
        STOP_WORDS = new HashSet<>(Arrays.asList(
            "a","about","above","after","again","against","all","also","am","an","and",
            "any","are","as","at","be","because","been","before","being","below",
            "between","both","but","by","can","cannot","could","did","do","does",
            "doing","down","during","each","few","for","from","further","get","got",
            "had","has","have","having","he","her","here","hers","herself","him",
            "himself","his","how","however","i","if","in","into","is","it","its",
            "itself","just","me","more","most","my","myself","no","nor","not","of",
            "off","on","once","only","or","other","our","ours","ourselves","out",
            "over","own","same","she","should","so","some","such","than","that","the",
            "their","theirs","them","themselves","then","there","these","they","this",
            "those","through","to","too","under","until","up","very","was","we","were",
            "what","when","where","which","while","who","whom","why","will","with",
            "would","you","your","yours","yourself","yourselves","use","used","using",
            "one","two","three","thus","hence","therefore","although","though","even",
            "every","many","much","well","yet","shall","may","might","must","need",
            "dare","now","ought","here","there","aren't","couldn't","didn't","doesn't",
            "don't","hadn't","hasn't","haven't","he'd","he'll","he's","here's","how's",
            "i'd","i'll","i'm","i've","isn't","it's","let's","mustn't","shan't",
            "she'd","she'll","she's","shouldn't","that's","there's","they'd","they'll",
            "they're","they've","wasn't","we'd","we'll","we're","we've","weren't",
            "what's","when's","where's","who's","why's","won't","wouldn't","you'd",
            "you'll","you're","you've"
        ));
    }

    private static final java.util.regex.Pattern TOKEN_SPLIT =
            java.util.regex.Pattern.compile("[^a-zA-Z0-9]+");

    // ── Public API ────────────────────────────────────────────────────────────

    public List<String> extract(String text) {
        return extract(text, DEFAULT_MAX_KEYWORDS);
    }

    public List<String> extract(String text, int maxKeywords) {
        if (text == null || text.isBlank()) return List.of();

        Map<String, Long> frequencies = Arrays.stream(TOKEN_SPLIT.split(text.toLowerCase()))
                .filter(t -> t.length() >= 3)
                .filter(t -> !STOP_WORDS.contains(t))
                .filter(t -> !isNumeric(t))
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

        return frequencies.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(maxKeywords)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private boolean isNumeric(String token) {
        for (char c : token.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }
}