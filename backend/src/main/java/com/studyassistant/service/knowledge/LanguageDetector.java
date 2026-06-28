package com.studyassistant.service.knowledge;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Detects the primary language of a document using stop-word frequency heuristics.
 *
 * <p><b>Single Responsibility:</b> language detection only. No AI or external libraries.
 *
 * <p><b>Approach:</b> counts how many of the 50 most frequent tokens in the document
 * appear in each language's high-frequency stop-word fingerprint. The language
 * whose fingerprint has the most matches wins. Ties default to {@code "en"}.
 *
 * <p>Returns BCP-47 language tags (e.g. {@code "en"}, {@code "de"}, {@code "fr"}).
 *
 * <p>Accuracy: sufficient for routing to multilingual AI providers in M04.
 * For production-grade detection, replace with a library such as
 * <a href="https://github.com/pemistahl/lingua">Lingua</a> without changing
 * this service's public interface.
 */
@Service
public class LanguageDetector {

    /** Minimum token sample size below which we default to "en". */
    private static final int MIN_SAMPLE_TOKENS = 20;

    /** Default BCP-47 tag when detection is inconclusive. */
    public static final String DEFAULT_LANGUAGE = "en";

    /**
     * High-frequency function-word fingerprints per language (BCP-47 tag → word set).
     * Words chosen for high discriminatory power across languages.
     */
    private static final Map<String, Set<String>> FINGERPRINTS = Map.of(
            "en", Set.of("the","and","of","to","in","is","it","that","this","with",
                         "for","are","as","was","at","be","by","from","or","an"),
            "de", Set.of("die","der","und","in","den","von","zu","das","mit","sich",
                         "des","auf","für","ist","im","dem","nicht","ein","eine","als"),
            "fr", Set.of("de","la","le","et","les","des","en","un","une","du",
                         "que","qui","est","il","dans","par","sur","au","ce","son"),
            "es", Set.of("de","la","el","en","y","que","los","se","del","las",
                         "un","por","con","una","su","para","es","al","lo","como"),
            "pt", Set.of("de","a","o","que","e","do","da","em","um","para",
                         "com","uma","os","no","se","na","por","mais","as","dos"),
            "it", Set.of("di","il","la","che","e","in","un","del","è","per",
                         "con","una","i","si","le","lo","sono","da","non","al")
    );

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Detect the primary language of the supplied text.
     *
     * @param fullText concatenated document text
     * @return BCP-47 language tag, never null; defaults to {@value #DEFAULT_LANGUAGE}
     */
    public String detect(String fullText) {
        if (fullText == null || fullText.isBlank()) return DEFAULT_LANGUAGE;

        // Tokenise and lower-case
        String[] tokens = fullText.toLowerCase().split("[^a-zA-ZÀ-ÿ]+");
        if (tokens.length < MIN_SAMPLE_TOKENS) return DEFAULT_LANGUAGE;

        // Count matches per language fingerprint
        String bestLang  = DEFAULT_LANGUAGE;
        long   bestScore = 0;

        for (var entry : FINGERPRINTS.entrySet()) {
            long score = 0;
            for (String token : tokens) {
                if (entry.getValue().contains(token)) score++;
            }
            if (score > bestScore) {
                bestScore = score;
                bestLang  = entry.getKey();
            }
        }

        return bestLang;
    }
}
