package com.studyassistant.controller;

import com.studyassistant.dto.UploadResponse;
import com.studyassistant.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * REST controller for document upload.
 *
 * <p>Intentionally thin – all validation and storage logic lives in
 * {@link UploadService}. The controller's only responsibilities are:
 * <ul>
 *   <li>Bind the HTTP multipart request to a {@link MultipartFile}.</li>
 *   <li>Delegate to the service.</li>
 *   <li>Return the appropriate HTTP response.</li>
 * </ul>
 *
 * <p>IOException is declared (not caught) so it propagates to
 * {@code GlobalExceptionHandler}, which maps it to HTTP 500 with a safe message.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    /**
     * Upload a single study document.
     *
     * <pre>
     * POST /api/upload
     * Content-Type: multipart/form-data
     * Body param:   file
     * </pre>
     *
     * @param file the document to upload
     * @return 200 with {@link UploadResponse} on success;
     *         400 via {@code GlobalExceptionHandler} on validation failure;
     *         500 on unexpected I/O error
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file) throws IOException {

        log.debug("Upload request received: name='{}', size={}, contentType='{}'",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        UploadResponse response = uploadService.store(file);
        return ResponseEntity.ok(response);
    }
}
