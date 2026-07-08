package com.studyassistant.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyassistant.dto.chat.ChatRequest;
import com.studyassistant.dto.chat.ChatResponse;
import com.studyassistant.dto.knowledge.KnowledgeChunk;
import com.studyassistant.dto.knowledge.KnowledgeDocument;
import com.studyassistant.exception.ResourceNotFoundException;
import com.studyassistant.service.processing.ProcessedStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Orchestrates the question-answering flow for M04A.
 *
 * <p><b>M04A strategy (full-context, no retrieval):</b>
 * All chunk text is concatenated into a single prompt and sent to the AI
 * provider. This is intentionally simple — chunk retrieval and ranking are
 * deferred to M04B to keep this module focused on proving the end-to-end flow.
 *
 * <p><b>Prompt structure:</b>
 * <pre>
 * You are an academic study assistant.
 * Answer ONLY using the supplied study material.
 * If the answer is not present, respond exactly:
 * 'I could not find that information in this document.'
 *
 * Study Material:
 * &lt;concatenated chunk text&gt;
 *
 * Question:
 * &lt;user question&gt;
 * </pre>
 *
 * <p>Single Responsibility: prompt assembly and response composition.
 * Knowledge loading is delegated to {@link ProcessedStorage} (path resolution)
 * + Jackson (deserialisation). AI communication is delegated to {@link AiProvider}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final AiProvider       aiProvider;
    private final ProcessedStorage processedStorage;
    private final ObjectMapper     objectMapper;

    /**
     * Answer a question using the knowledge document for the given upload.
     *
     * @param request contains {@code uploadId} and {@code question}
     * @return {@link ChatResponse} with the AI answer and provider metadata
     * @throws ResourceNotFoundException if no {@code knowledge.json} exists for the uploadId
     * @throws AiProviderException       if the AI provider fails
     */
    public ChatResponse answer(ChatRequest request) throws IOException {
        String uploadId = request.getUploadId();
        log.info("Chat request – uploadId={}, question-length={}", uploadId, request.getQuestion().length());

        // ── 1. Load knowledge.json ────────────────────────────────────────────
        Path knowledgePath = processedStorage.getOutputRoot(uploadId).resolve("knowledge.json");

        if (!knowledgePath.toFile().exists()) {
            throw new ResourceNotFoundException(
                    "No processed knowledge found for uploadId '" + uploadId +
                    "'. Please upload and process a document first.");
        }

        KnowledgeDocument knowledge;
        try {
            knowledge = objectMapper.readValue(knowledgePath.toFile(), KnowledgeDocument.class);
        } catch (IOException e) {
            log.error("Failed to read knowledge.json for uploadId={}", uploadId, e);
            throw new AiProviderException("Could not load the document knowledge base. Please re-upload the document.");
        }

        if (knowledge.getChunks().isEmpty()) {
            throw new ResourceNotFoundException(
                    "The document for uploadId '" + uploadId + "' has no extractable content.");
        }

        // ── 2. Assemble study material from all chunks (M04A – no retrieval) ─
        log.debug("Assembling prompt from {} chunks for uploadId={}", knowledge.getChunks().size(), uploadId);

        String studyMaterial = knowledge.getChunks().stream()
                .map(KnowledgeChunk::getText)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n\n---\n\n"));

        // ── 3. Build prompt ───────────────────────────────────────────────────
        String prompt = buildPrompt(studyMaterial, request.getQuestion());
        log.debug("Prompt assembled – total-length={} chars", prompt.length());

        // ── 4. Call AI provider ───────────────────────────────────────────────
        String answer = aiProvider.ask(prompt);

        // ── 5. Compose response ───────────────────────────────────────────────
        return ChatResponse.builder()
                .answer(answer)
                .provider(aiProvider.providerName())
                .model(aiProvider.modelName())
                .timestamp(Instant.now())
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String buildPrompt(String studyMaterial, String question) {
        return """
                You are an academic study assistant.
                Answer ONLY using the supplied study material below.
                If the answer is not present in the study material, respond exactly:
                'I could not find that information in this document.'
                Do not use any external knowledge. Do not make up information.

                Study Material:
                %s

                Question:
                %s
                """.formatted(studyMaterial, question);
    }
}
