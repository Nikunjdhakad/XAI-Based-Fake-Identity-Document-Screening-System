package com.verilens.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verilens.dto.AiManipulationResponse;
import com.verilens.dto.AnalysisTaskResponse;
import com.verilens.dto.FaceVerificationResponse;
import com.verilens.dto.ForensicResultResponse;
import com.verilens.dto.IdentityConsistencyResponse;
import com.verilens.dto.MetadataResultResponse;
import com.verilens.dto.OcrResultResponse;
import com.verilens.dto.RiskAssessmentResponse;
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
 * Phase 6: Identity consistency (rule-based, depends on OCR output).
 * Phase 7: Image forensics (OpenCV — ELA, noise, edge analysis).
 * Phase 8: Face verification (OpenCV Haar cascade + HOG features).
 * Phase 9: AI Manipulation screening (frequency-domain + statistical baseline).
 * Phase 10: Evidence Fusion & Risk Scoring.
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
    private final IdentityConsistencyResultRepository identityResultRepository;
    private final ForensicResultRepository forensicResultRepository;
    private final FaceVerificationResultRepository faceResultRepository;
    private final AiManipulationResultRepository aiManipulationResultRepository;
    private final AiServiceClient aiServiceClient;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;
    private final EvidenceFusionService evidenceFusionService;

    public AnalysisService(
            AnalysisTaskRepository analysisTaskRepository,
            DocumentRepository documentRepository,
            DocumentFileRepository documentFileRepository,
            OcrResultRepository ocrResultRepository,
            MetadataResultRepository metadataResultRepository,
            IdentityConsistencyResultRepository identityResultRepository,
            ForensicResultRepository forensicResultRepository,
            FaceVerificationResultRepository faceResultRepository,
            AiManipulationResultRepository aiManipulationResultRepository,
            AiServiceClient aiServiceClient,
            StorageService storageService,
            ObjectMapper objectMapper,
            EvidenceFusionService evidenceFusionService) {
        this.analysisTaskRepository = analysisTaskRepository;
        this.documentRepository = documentRepository;
        this.documentFileRepository = documentFileRepository;
        this.ocrResultRepository = ocrResultRepository;
        this.metadataResultRepository = metadataResultRepository;
        this.identityResultRepository = identityResultRepository;
        this.forensicResultRepository = forensicResultRepository;
        this.faceResultRepository = faceResultRepository;
        this.aiManipulationResultRepository = aiManipulationResultRepository;
        this.aiServiceClient = aiServiceClient;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
        this.evidenceFusionService = evidenceFusionService;
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
        task.setRequestedModules(modules != null ? modules : "OCR,METADATA,IDENTITY,FORENSICS,FACE,AI_MANIPULATION");

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

    /**
     * Get the identity consistency result for the latest analysis of a document.
     */
    @Transactional(readOnly = true)
    public IdentityConsistencyResponse getIdentityResult(UUID documentId, UUID userId) {
        documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found or access denied"));

        IdentityConsistencyResult ident = identityResultRepository
                .findTopByDocumentIdOrderByCreatedAtDesc(documentId)
                .orElseThrow(() -> new RuntimeException("No identity consistency result found for this document"));

        return IdentityConsistencyResponse.fromEntity(ident);
    }

    /**
     * Get the forensic result for the latest analysis of a document.
     */
    @Transactional(readOnly = true)
    public ForensicResultResponse getForensicResult(UUID documentId, UUID userId) {
        documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found or access denied"));

        ForensicResult forensics = forensicResultRepository
                .findTopByDocumentIdOrderByCreatedAtDesc(documentId)
                .orElseThrow(() -> new RuntimeException("No forensic result found for this document"));

        return ForensicResultResponse.fromEntity(forensics);
    }

    /**
     * Get the face verification result for the latest analysis of a document.
     */
    @Transactional(readOnly = true)
    public FaceVerificationResponse getFaceResult(UUID documentId, UUID userId) {
        documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found or access denied"));
        FaceVerificationResult face = faceResultRepository
                .findTopByDocumentIdOrderByCreatedAtDesc(documentId)
                .orElseThrow(() -> new RuntimeException("No face verification result found for this document"));
        return FaceVerificationResponse.fromEntity(face);
    }

    /**
     * Get the risk assessment for the latest analysis of a document.
     */
    @Transactional(readOnly = true)
    public RiskAssessmentResponse getRiskAssessment(UUID documentId, UUID userId) {
        return evidenceFusionService.getRiskAssessment(documentId, userId);
    }

    /**
     * Get the AI manipulation screening result for the latest analysis of a document.
     */
    @Transactional(readOnly = true)
    public AiManipulationResponse getAiManipulationResult(UUID documentId, UUID userId) {
        documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found or access denied"));
        AiManipulationResult aiManip = aiManipulationResultRepository
                .findTopByDocumentIdOrderByCreatedAtDesc(documentId)
                .orElseThrow(() -> new RuntimeException("No AI manipulation result found for this document"));
        return AiManipulationResponse.fromEntity(aiManip);
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

            // Find optional reference face file
            byte[] refFaceBytes = null;
            String refFaceName = null;
            String refFaceMime = null;
            java.util.Optional<DocumentFile> refFaceOpt = documentFileRepository
                    .findByDocumentId(document.getId())
                    .stream()
                    .filter(f -> f.getFileType() == FileType.REFERENCE_FACE)
                    .findFirst();
            if (refFaceOpt.isPresent()) {
                DocumentFile refFile = refFaceOpt.get();
                refFaceBytes = Files.readAllBytes(storageService.resolveFile(refFile.getStoragePath()));
                refFaceName = refFile.getOriginalFilename();
                refFaceMime = refFile.getMimeType();
            }

            // Dispatch to Python AI service
            String requestedModules = task.getRequestedModules() != null
                    ? task.getRequestedModules() : "OCR,METADATA,IDENTITY,FORENSICS,FACE,AI_MANIPULATION";

            Map<String, Object> aiResponse = aiServiceClient.analyze(
                    document.getId().toString(),
                    fileBytes,
                    docFile.getOriginalFilename(),
                    docFile.getMimeType(),
                    requestedModules,
                    refFaceBytes,
                    refFaceName,
                    refFaceMime);

            if (aiResponse == null) {
                safeMarkFailed(taskId, "AI service returned no response");
                return;
            }

            // Persist per-module results
            persistOcrResult(task, document, aiResponse);
            persistMetadataResult(task, document, aiResponse);
            persistIdentityResult(task, document, aiResponse);
            persistForensicResult(task, document, aiResponse);
            persistFaceResult(task, document, aiResponse);
            persistAiManipulationResult(task, document, aiResponse);

            // Phase 10: run evidence fusion after all module results are persisted
            evidenceFusionService.fuse(task, document);

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
    private void persistFaceResult(AnalysisTask task, Document document,
                                    Map<String, Object> aiResponse) {
        try {
            Object modulesObj = aiResponse.get("modules");
            if (!(modulesObj instanceof Map)) return;
            Map<String, Object> modules = (Map<String, Object>) modulesObj;
            Object faceObj = modules.get("faceVerification");
            if (!(faceObj instanceof Map)) return;
            Map<String, Object> faceData = (Map<String, Object>) faceObj;

            FaceVerificationResult face = new FaceVerificationResult();
            face.setTask(task);
            face.setDocument(document);
            face.setStatus((String) faceData.getOrDefault("status", "FAILED"));
            face.setNote((String) faceData.get("note"));
            face.setReason((String) faceData.get("reason"));
            face.setErrorMessage((String) faceData.get("error"));

            Object sim = faceData.get("similarity");
            if (sim instanceof Number n) {
                face.setSimilarity(BigDecimal.valueOf(n.doubleValue()));
            }
            Object thresh = faceData.get("threshold");
            if (thresh instanceof Number n) {
                face.setThreshold(BigDecimal.valueOf(n.doubleValue()));
            }
            Object docFace = faceData.get("documentFaceDetected");
            if (docFace instanceof Boolean b) face.setDocumentFaceDetected(b);
            Object refFace = faceData.get("referenceFaceDetected");
            if (refFace instanceof Boolean b) face.setReferenceFaceDetected(b);

            Object ptms = faceData.get("processingTimeMs");
            if (ptms instanceof Number n) face.setProcessingTimeMs(n.longValue());

            faceResultRepository.save(face);
            log.debug("Persisted face result for task {} — status: {}", task.getId(), face.getStatus());
        } catch (Exception e) {
            log.error("Failed to persist face result for task {}: {}", task.getId(), e.getMessage());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void persistAiManipulationResult(AnalysisTask task, Document document,
                                              Map<String, Object> aiResponse) {
        try {
            Object modulesObj = aiResponse.get("modules");
            if (!(modulesObj instanceof Map)) return;
            Map<String, Object> modules = (Map<String, Object>) modulesObj;
            Object aiManipObj = modules.get("aiManipulation");
            if (!(aiManipObj instanceof Map)) return;
            Map<String, Object> aiManipData = (Map<String, Object>) aiManipObj;

            AiManipulationResult aiManip = new AiManipulationResult();
            aiManip.setTask(task);
            aiManip.setDocument(document);
            aiManip.setStatus((String) aiManipData.getOrDefault("status", "FAILED"));
            aiManip.setErrorMessage((String) aiManipData.get("error"));
            aiManip.setManipulationType((String) aiManipData.get("manipulationType"));

            Object riskScore = aiManipData.get("riskScore");
            if (riskScore instanceof Number n) {
                aiManip.setRiskScore(BigDecimal.valueOf(n.doubleValue()));
            }

            Object conf = aiManipData.get("confidence");
            if (conf instanceof Number n) {
                aiManip.setConfidence(BigDecimal.valueOf(n.doubleValue()));
            }

            Object ptms = aiManipData.get("processingTimeMs");
            if (ptms instanceof Number n) aiManip.setProcessingTimeMs(n.longValue());

            // Serialise findings, regions, and model info to JSON
            Object findings = aiManipData.get("findings");
            if (findings != null) {
                try {
                    aiManip.setFindingsJson(objectMapper.writeValueAsString(findings));
                } catch (JsonProcessingException ex) {
                    log.warn("Could not serialise AI manipulation findings: {}", ex.getMessage());
                }
            }

            Object regions = aiManipData.get("suspiciousRegions");
            if (regions != null) {
                try {
                    aiManip.setSuspiciousRegionsJson(objectMapper.writeValueAsString(regions));
                } catch (JsonProcessingException ex) {
                    log.warn("Could not serialise AI manipulation regions: {}", ex.getMessage());
                }
            }

            Object modelInfo = aiManipData.get("modelInfo");
            if (modelInfo != null) {
                try {
                    aiManip.setModelInfoJson(objectMapper.writeValueAsString(modelInfo));
                } catch (JsonProcessingException ex) {
                    log.warn("Could not serialise AI manipulation model info: {}", ex.getMessage());
                }
            }

            aiManipulationResultRepository.save(aiManip);
            log.debug("Persisted AI manipulation result for task {} — status: {}", task.getId(), aiManip.getStatus());
        } catch (Exception e) {
            log.error("Failed to persist AI manipulation result for task {}: {}", task.getId(), e.getMessage());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void persistForensicResult(AnalysisTask task, Document document,
                                       Map<String, Object> aiResponse) {
        try {
            Object modulesObj = aiResponse.get("modules");
            if (!(modulesObj instanceof Map)) return;

            Map<String, Object> modules = (Map<String, Object>) modulesObj;
            Object forensicsObj = modules.get("forensics");
            if (!(forensicsObj instanceof Map)) return;

            Map<String, Object> forensicsData = (Map<String, Object>) forensicsObj;

            ForensicResult forensics = new ForensicResult();
            forensics.setTask(task);
            forensics.setDocument(document);
            forensics.setStatus((String) forensicsData.getOrDefault("status", "FAILED"));
            forensics.setErrorMessage((String) forensicsData.get("error"));

            Object riskScore = forensicsData.get("riskScore");
            if (riskScore instanceof Number n) {
                forensics.setRiskScore(BigDecimal.valueOf(n.doubleValue()));
            }

            Object elaMean = forensicsData.get("elaMeanError");
            if (elaMean instanceof Number n) {
                forensics.setElaMeanError(BigDecimal.valueOf(n.doubleValue()));
            }

            Object noiseMean = forensicsData.get("noiseMean");
            if (noiseMean instanceof Number n) {
                forensics.setNoiseMean(BigDecimal.valueOf(n.doubleValue()));
            }

            Object noiseStd = forensicsData.get("noiseStd");
            if (noiseStd instanceof Number n) {
                forensics.setNoiseStd(BigDecimal.valueOf(n.doubleValue()));
            }

            Object edgeMean = forensicsData.get("edgeMean");
            if (edgeMean instanceof Number n) {
                forensics.setEdgeMean(BigDecimal.valueOf(n.doubleValue()));
            }

            Object edgeStd = forensicsData.get("edgeStd");
            if (edgeStd instanceof Number n) {
                forensics.setEdgeStd(BigDecimal.valueOf(n.doubleValue()));
            }

            Object ptms = forensicsData.get("processingTimeMs");
            if (ptms instanceof Number n) {
                forensics.setProcessingTimeMs(n.longValue());
            }

            // Serialise findings and regions to JSON
            Object findings = forensicsData.get("findings");
            if (findings != null) {
                try {
                    forensics.setFindingsJson(objectMapper.writeValueAsString(findings));
                } catch (JsonProcessingException ex) {
                    log.warn("Could not serialise forensics findings: {}", ex.getMessage());
                }
            }

            Object regions = forensicsData.get("suspiciousRegions");
            if (regions != null) {
                try {
                    forensics.setSuspiciousRegionsJson(objectMapper.writeValueAsString(regions));
                } catch (JsonProcessingException ex) {
                    log.warn("Could not serialise forensics regions: {}", ex.getMessage());
                }
            }

            forensicResultRepository.save(forensics);
            log.debug("Persisted forensic result for task {} — status: {}", task.getId(), forensics.getStatus());

        } catch (Exception e) {
            log.error("Failed to persist forensic result for task {}: {}", task.getId(), e.getMessage());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void persistIdentityResult(AnalysisTask task, Document document,
                                        Map<String, Object> aiResponse) {
        try {
            Object modulesObj = aiResponse.get("modules");
            if (!(modulesObj instanceof Map)) return;

            Map<String, Object> modules = (Map<String, Object>) modulesObj;
            Object identObj = modules.get("identity");
            if (!(identObj instanceof Map)) return;

            Map<String, Object> identData = (Map<String, Object>) identObj;

            IdentityConsistencyResult ident = new IdentityConsistencyResult();
            ident.setTask(task);
            ident.setDocument(document);
            ident.setStatus((String) identData.getOrDefault("status", "FAILED"));
            ident.setErrorMessage((String) identData.get("error"));

            Object consistent = identData.get("consistent");
            if (consistent instanceof Boolean b) {
                ident.setConsistent(b);
            }

            Object ptms = identData.get("processingTimeMs");
            if (ptms instanceof Number n) {
                ident.setProcessingTimeMs(n.longValue());
            }

            // Extract failed check counts from nested failedChecks object
            Object failedObj = identData.get("failedChecks");
            if (failedObj instanceof Map fc) {
                Object high = fc.get("high");
                Object medium = fc.get("medium");
                Object low = fc.get("low");
                if (high instanceof Number n) ident.setFailedHigh(n.intValue());
                if (medium instanceof Number n) ident.setFailedMedium(n.intValue());
                if (low instanceof Number n) ident.setFailedLow(n.intValue());
            }

            // Serialise checks list to JSON
            Object checks = identData.get("checks");
            if (checks != null) {
                try {
                    ident.setChecksJson(objectMapper.writeValueAsString(checks));
                } catch (JsonProcessingException ex) {
                    log.warn("Could not serialise identity checks: {}", ex.getMessage());
                }
            }

            identityResultRepository.save(ident);
            log.debug("Persisted identity result for task {} — status: {}", task.getId(), ident.getStatus());

        } catch (Exception e) {
            log.error("Failed to persist identity result for task {}: {}", task.getId(), e.getMessage());
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
