package com.studyassistant.model;

/**
 * Outcome of a document processing run.
 *
 * <ul>
 *   <li>{@link #SUCCESS}         – all pages processed, all images extracted.</li>
 *   <li>{@link #PARTIAL_SUCCESS} – processing completed with recoverable warnings
 *                                  (e.g. some pages had no text, or some images
 *                                  could not be decoded).</li>
 *   <li>{@link #FAILED}          – fatal error; no usable output was produced.</li>
 * </ul>
 */
public enum ProcessingStatus {
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILED
}
