package com.studyassistant.service.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Associates extracted image files with knowledge chunks.
 *
 * <p><b>Single Responsibility:</b> image-to-chunk binding only.
 * No text processing, no I/O beyond scanning the images directory.
 *
 * <p><b>Algorithm:</b>
 * <ol>
 *   <li>Scan {@code processed/<uploadId>/images/} for all image files.</li>
 *   <li>Parse the page number from each filename using the M03 naming convention
 *       {@code page-N-img-M.<ext>}.</li>
 *   <li>For each image, find the chunk whose {@code pages} list contains that
 *       page number. If no exact match exists, fall back to the chunk whose
 *       page range is closest (nearest start page).</li>
 *   <li>Return a map from chunk index (0-based position in the raw-chunk list)
 *       to the filenames of images associated with that chunk.</li>
 * </ol>
 *
 * <p>Images that cannot be parsed or matched are silently skipped with a warning
 * log, consistent with the recoverable-error philosophy established in M03.
 */
@Slf4j
@Service
public class ImageAssociator {

    /** Matches M03-generated filenames: {@code page-3-img-2.png}. */
    private static final Pattern IMAGE_FILENAME_PATTERN =
            Pattern.compile("page-(\\d+)-img-(\\d+)\\..+", Pattern.CASE_INSENSITIVE);

    /**
     * Scans the images directory and maps each image to the chunk whose page
     * range best covers the image's source page.
     *
     * @param imagesDir  absolute path to {@code processed/<uploadId>/images/}
     * @param chunkPages list of page-number lists, one per raw chunk (0-based index)
     * @return map from chunk index → list of image filenames for that chunk
     */
    public Map<Integer, List<String>> associate(Path imagesDir, List<List<Integer>> chunkPages) {
        Map<Integer, List<String>> result = new HashMap<>();
        // Initialise empty lists so every chunk index is present
        for (int i = 0; i < chunkPages.size(); i++) {
            result.put(i, new ArrayList<>());
        }

        if (!Files.isDirectory(imagesDir)) {
            log.debug("Images directory does not exist – no images to associate: {}", imagesDir);
            return result;
        }

        List<Path> imageFiles;
        try (Stream<Path> stream = Files.list(imagesDir)) {
            imageFiles = stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.warn("Could not list images directory: {}", imagesDir, e);
            return result;
        }

        for (Path imageFile : imageFiles) {
            String filename = imageFile.getFileName().toString();
            Matcher m = IMAGE_FILENAME_PATTERN.matcher(filename);

            if (!m.matches()) {
                log.debug("Image filename does not match expected pattern, skipping: {}", filename);
                continue;
            }

            int imagePage = Integer.parseInt(m.group(1));
            int chunkIdx  = findBestChunkIndex(imagePage, chunkPages);

            if (chunkIdx >= 0) {
                result.get(chunkIdx).add(filename);
                log.debug("Associated image '{}' (page {}) → chunk index {}", filename, imagePage, chunkIdx);
            } else {
                log.warn("Could not associate image '{}' (page {}) with any chunk", filename, imagePage);
            }
        }

        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Finds the chunk index whose page list contains {@code page}, or — if no
     * exact match exists — the chunk whose start page is closest to {@code page}.
     *
     * @return 0-based chunk index, or -1 if {@code chunkPages} is empty
     */
    private int findBestChunkIndex(int page, List<List<Integer>> chunkPages) {
        if (chunkPages.isEmpty()) return -1;

        // Exact match first
        for (int i = 0; i < chunkPages.size(); i++) {
            if (chunkPages.get(i).contains(page)) return i;
        }

        // Nearest-start-page fallback
        int bestIdx  = 0;
        int bestDist = Integer.MAX_VALUE;

        for (int i = 0; i < chunkPages.size(); i++) {
            List<Integer> pages = chunkPages.get(i);
            if (pages.isEmpty()) continue;
            int startPage = Collections.min(pages);
            int dist      = Math.abs(startPage - page);
            if (dist < bestDist) {
                bestDist = dist;
                bestIdx  = i;
            }
        }

        return bestIdx;
    }
}
