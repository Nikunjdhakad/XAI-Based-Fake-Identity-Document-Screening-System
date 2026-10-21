package com.verilens.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verilens.dto.AnalysisTaskResponse;
import com.verilens.dto.MetadataResultResponse;
import com.verilens.dto.OcrResultResponse;
import com.verilens.entity.*;
import com.verilens.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Analysis service — manages analysis task lifecycle and dispatches AI processing.
 *
 * Phase 4: OCR is connected via AiServiceClient → Python AI service (Tesseract).
 * Phase 5: Metadata analysis connected (Pillow + PyMuPDF).
 * Analysis runs asynchronously so HTTP responses are not blocked.
 */
@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final AnalysisTaskRepository analysisTaskRepository;
    private final DocumentRepository documentRepository;
    private final DocumentFileRepository documentFileRepository;
    private final OcrResultRepository ocrResultRepository;
    private final MetadataResultRepository metadataResultRepository;
    private final AiServiceClient aiServiceClient;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;

    public AnalysisService(
            AnalysisTaskRepository analysisTaskRepository,
            DocumentRepository documentRepository,
            DocumentFileRepository documentFileRepository,
            OcrResultRepository ocrResultRepository,
            MetadataResultRepository metadataResultRepository,
            AiServiceClient aiServiceClient,
            StorageService storageService,
            ObjectMapper objectMapper) {
        this.analysisTaskRepository = analysisTaskRepository;
        this.documentRepository = documentRepository;
        this.documentFileRepository = documentFileRepository;
        this.ocrResultRepository = ocrResultRepository;
        this.metadataResultRepository = metadataResultRepository;
        this.aiServiceClient = aiServiceClient;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Create a new analysis task and dispatch async processing.
     * Returns immediately — the frontend polls /analysis/status for updates.
     */
    @Transactional
    public AnalysisTaskResponse createAnalysisTask(UUID documentId, UUID userId, String modules) {
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found or access denied"));

        document.setStatus(DocumentStatus.QUEUED);
        documentRepository.save(document);

        AnalysisTask task = new AnalysisTask();
        task.setDocument(document);
        task.setStatus(AnalysisTaskStatus.QUEUED);
        task.setRequestedModules(modules != null ? modules : "OCR,METADATA");

        AnalysisTask saved = analysisTaskRepository.save(task);

        // Kick off async — returns before analysis completes
        runAnalysisAsync(saved.getId());

        return AnalysisTaskResponse.fromEntity(saved);
    }

    /**
     * Get the latest analysis task status for a document.
     */
    @Transactional(readOnly = true)
    public AnalysisTaskResponse getAnalysisStatus(UUID documentId, UUID userId) {
        documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found or access denied"));

        AnalysisTask task = analysisTaskRepository
                .findTopByDocumentIdOrderByCreatedAtDesc(documentId)
                .orElseThrow(() -> new RuntimeException("No analysis task found for this document"));

        return AnalysisTaskResponse.fromEntity(task);
    }

    /**
     * Get all analysis tasks for a document (history).
     */
    @Transactional(readOnly = true)
    public List<AnalysisTaskResponse> getAnalysisTasks(UUID documentId, UUID userId) {
        documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found or access denied"));

        return analysisTaskRepository.findByDocumentId(documentId)
                .stream()
                .map(AnalysisTaskResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get the OCR result for the latest analysis of a document.
     */
    @Transactional(readOnly = true)
    public OcrResultResponse getOcrResult(UUID documentId, UUID userId) {
        documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found or access denied"));

        OcrResult ocr = ocrResultRepository
                .findTopByDocumentIdOrderByCreatedAtDesc(documentId)
                .orElseThrow(() -> new RuntimeException("No OCR result found for this document"));

        return OcrResultResponse.fromEntity(ocr);
    }

    /**
     * Get the metadata analysis result for the latest analysis of a document.
     */
    @Transactional(readOnly = true)
    public MetadataResultResponse getMetadataResult(UUID documentId, UUID userId) {
        documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found or access denied"));

        MetadataResult meta = metadataResultRepository
                .findTopByDocumentIdOrderByCreatedAtDesc(documentId)
                .orElseThrow(() -> new RuntimeException("No metadata result found for this document"));

        return MetadataResultResponse.fromEntity(meta);
    }

    // -------------------------------------------------------------------------
    // Async analysis pipeline
    // -------------------------------------------------------------------------

    /**
     * Public entry point for async execution.
     * Spring @Async requires a public method on a managed bean.
     */
    @Async("analysisExecutor")
    public void runAnalysisAsync(UUID taskId) {
        try {
            runAnalysis(taskId);
        } catch (Exception e) {
            log.error("Unhandled exception in analysis task {}: {}", taskId, e.getMessage(), e);
            safeMarkFailed(taskId, "Internal error: " + e.getMessage());
        }
    }

    private void runAnalysis(UUID taskId) {
        AnalysisTask task = analysisTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        Document document = task.getDocument();

        // Transition to PROCESSING
        task.setStatus(AnalysisTaskStatus.PROCESSING);
        task.setStartedAt(Instant.now());
        analysisTaskRepository.save(task);
        document.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(document);

        Instant start = Instant.now();

        try {
            // Locate the primary document file
            DocumentFile docFile = documentFileRepository
                    .findByDocumentId(document.getId())
                    .stream()
                    .filter(f -> f.getFileType() == FileType.DOCUMENT)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No document file found for " + document.getId()));

            // Read bytes from local storage
            byte[] fileBytes = Files.readAllBytes(
                    storageService.resolveFile(docFile.getStoragePath()));

            // Dispatch to Python AI service
            String requestedModules = task.getRequestedModules() != null
                    ? task.getRequestedModules() : "OCR";

            Map<String, Object> aiResponse = aiServiceClient.analyze(
                    document.getId().toString(),
                    fileBytes,
                    docFile.getOriginalFilename(),
                    docFile.getMimeType(),
                    requestedModules);

            if (aiResponse == null) {
                safeMarkFailed(taskId, "AI service returned no response");
                return;
            }

            // Persist per-module results
            persistOcrResult(task, document, aiResponse);
            persistMetadataResult(task, document, aiResponse);

            // Derive final status
            String aiStatus = (String) aiResponse.getOrDefault("status", "FAILED");
            AnalysisTaskStatus finalTaskStatus = switch (aiStatus) {
                case "COMPLETED"           -> AnalysisTaskStatus.COMPLETED;
                case "PARTIALLY_COMPLETED" -> AnalysisTaskStatus.PARTIALLY_COMPLETED;
                default                    -> AnalysisTaskStatus.FAILED;
            };
            DocumentStatus finalDocStatus = switch (finalTaskStatus) {
                case COMPLETED           -> DocumentStatus.COMPLETED;
                case PARTIALLY_COMPLETED -> DocumentStatus.PARTIALLY_COMPLETED;
                default                  -> DocumentStatus.FAILED;
            };

            long processingMs = Instant.now().toEpochMilli() - start.toEpochMilli();
            task.setStatus(finalTaskStatus);
            task.setCompletedAt(Instant.now());
            task.setProcessingTimeMs(processingMs);
            analysisTaskRepository.save(task);

            document.setStatus(finalDocStatus);
            documentRepository.save(document);

            log.info("Analysis task {} completed in {}ms — status: {}", taskId, processingMs, finalTaskStatus);

        } catch (IOException e) {
            log.error("IO error reading document file for task {}: {}", taskId, e.getMessage());
            safeMarkFailed(taskId, "Could not read document file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Analysis failed for task {}: {}", taskId, e.getMessage(), e);
            safeMarkFailed(taskId, e.getMessage());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void persistOcrResult(AnalysisTask task, Document document,
                                   Map<String, Object> aiResponse) {
        try {
            Object modulesObj = aiResponse.get("modules");
            if (!(modulesObj instanceof Map)) return;

            Map<String, Object> modules = (Map<String, Object>) modulesObj;
            Object ocrObj = modules.get("ocr");
            if (!(ocrObj instanceof Map)) return;

            Map<String, Object> ocrData = (Map<String, Object>) ocrObj;

            OcrResult ocr = new OcrResult();
            ocr.setTask(task);
            ocr.setDocument(document);
            ocr.setStatus((String) ocrData.getOrDefault("status", "FAILED"));
            ocr.setExtractedText((String) ocrData.get("extractedText"));
            ocr.setErrorMessage((String) ocrData.get("error"));

            Object conf = ocrData.get("overallConfidence");
            if (conf instanceof Number n) {
                ocr.setOverallConfidence(BigDecimal.valueOf(n.doubleValue()));
            }
            Object pages = ocrData.get("pageCount");
            if (pages instanceof Number n) {
                ocr.setPageCount(n.intValue());
            }
            Object ptms = ocrData.get("processingTimeMs");
            if (ptms instanceof Number n) {
                ocr.setProcessingTimeMs(n.longValue());
            }

            // Serialise words list to JSON string for storage
            Object words = ocrData.get("words");
            if (words != null) {
                try {
                    ocr.setWordsJson(objectMapper.writeValueAsString(words));
                } catch (JsonProcessingException ex) {
                    log.warn("Could not serialise OCR words to JSON: {}", ex.getMessage());
                }
            }

            ocrResultRepository.save(ocr);
            log.debug("Persisted OCR result for task {} — status: {}", task.getId(), ocr.getStatus());

        } catch (Exception e) {
            log.error("Failed to persist OCR result for task {}: {}", task.getId(), e.getMessage());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void persistMetadataResult(AnalysisTask task, Document document,
                                        Map<String, Object> aiResponse) {
        try {
            Object modulesObj = aiResponse.get("modules");
            if (!(modulesObj instanceof Map)) return;

            Map<String, Object> modules = (Map<String, Object>) modulesObj;
            Object metaObj = modules.get("metadata");
            if (!(metaObj instanceof Map)) return;

            Map<String, Object> metaData = (Map<String, Object>) metaObj;

            MetadataResult meta = new MetadataResult();
            meta.setTask(task);
            meta.setDocument(document);
            meta.setStatus((String) metaData.getOrDefault("status", "FAILED"));
            meta.setErrorMessage((String) metaData.get("error"));

            Object avail = metaData.get("available");
            if (avail instanceof Boolean b) {
                meta.setAvailable(b);
            }

            Object score = metaData.get("evidenceScore");
            if (score instanceof Number n) {
                meta.setEvidenceScore(BigDecimal.valueOf(n.doubleValue()));
            }

            Object ptms = metaData.get("processingTimeMs");
            if (ptms instanceof Number n) {
                meta.setProcessingTimeMs(n.longValue());
            }

            Object props = metaData.get("properties");
            if (props != null) {
                try {
                    meta.setPropertiesJson(objectMapper.writeValueAsString(props));
                } catch (JsonProcessingException ex) {
                    log.warn("Could not serialise metadata properties: {}", ex.getMessage());
                }
            }

            Object anomalies = metaData.get("anomalies");
            if (anomalies != null) {
                try {
                    meta.setAnomaliesJson(objectMapper.writeValueAsString(anomalies));
                } catch (JsonProcessingException ex) {
                    log.warn("Could not serialise metadata anomalies: {}", ex.getMessage());
                }
            }

            metadataResultRepository.save(meta);
            log.debug("Persisted metadata result for task {} — status: {}", task.getId(), meta.getStatus());

        } catch (Exception e) {
            log.error("Failed to persist metadata result for task {}: {}", task.getId(), e.getMessage());
        }
    }

    private void safeMarkFailed(UUID taskId, String errorMessage) {
        try {
            analysisTaskRepository.findById(taskId).ifPresent(task -> {
                task.setStatus(AnalysisTaskStatus.FAILED);
                task.setCompletedAt(Instant.now());
                task.setErrorMessage(errorMessage);
                analysisTaskRepository.save(task);

                Document doc = task.getDocument();
                if (doc != null) {
                    doc.setStatus(DocumentStatus.FAILED);
                    documentRepository.save(doc);
                }
            });
        } catch (Exception e) {
            log.error("Could not mark task {} as failed: {}", taskId, e.getMessage());
        }
    }
}
