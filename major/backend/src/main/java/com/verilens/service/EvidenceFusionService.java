package com.verilens.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verilens.dto.RiskAssessmentResponse;
import com.verilens.entity.*;
import com.verilens.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Evidence Fusion Service — Phase 10.
 *
 * Combines the evidence from all six analysis modules into a single
 * RiskAssessment with:
 *  - Numeric score (0.0–1.0)
 *  - Risk level: LOW / MEDIUM / HIGH
 *  - Human-readable explanations
 *  - Per-module contributions
 *  - Scoring version for auditability
 *
 * Design principles (from specification):
 *  - Normalized module scores (0.0–1.0)
 *  - Configurable, auditable weights
 *  - Missing / FAILED / NOT_APPLICABLE modules handled explicitly
 *  - Confidence-aware aggregation
 *  - Scoring version stored for auditability
 *
 * IMPORTANT: These thresholds are initial estimates (v1.0.0).
 * They must be validated and refined during Phase 13 evaluation.
 * Never present thresholds as scientifically validated before experiments support them.
 */
@Service
public class EvidenceFusionService {

    private static final Logger log = LoggerFactory.getLogger(EvidenceFusionService.class);

    // ---------------------------------------------------------------------------
    // Scoring configuration v1.0.0
    // Weights are intentionally transparent and auditable.
    // They will be refined in Phase 13 after experimental evaluation.
    // ---------------------------------------------------------------------------
    private static final String SCORING_VERSION = "1.0.0";

    private static final double W_FORENSICS        = 0.25;
    private static final double W_AI_MANIPULATION  = 0.25;
    private static final double W_METADATA         = 0.15;
    private static final double W_IDENTITY         = 0.15;
    private static final double W_OCR              = 0.10;
    private static final double W_FACE             = 0.10;

    private static final double THRESHOLD_MEDIUM = 0.34;
    private static final double THRESHOLD_HIGH   = 0.67;

    private final OcrResultRepository ocrRepo;
    private final MetadataResultRepository metadataRepo;
    private final IdentityConsistencyResultRepository identityRepo;
    private final ForensicResultRepository forensicsRepo;
    private final FaceVerificationResultRepository faceRepo;
    private final AiManipulationResultRepository aiManipRepo;
    private final RiskAssessmentRepository riskRepo;
    private final DocumentRepository documentRepo;
    private final ObjectMapper objectMapper;

    public EvidenceFusionService(
            OcrResultRepository ocrRepo,
            MetadataResultRepository metadataRepo,
            IdentityConsistencyResultRepository identityRepo,
            ForensicResultRepository forensicsRepo,
            FaceVerificationResultRepository faceRepo,
            AiManipulationResultRepository aiManipRepo,
            RiskAssessmentRepository riskRepo,
            DocumentRepository documentRepo,
            ObjectMapper objectMapper) {
        this.ocrRepo = ocrRepo;
        this.metadataRepo = metadataRepo;
        this.identityRepo = identityRepo;
        this.forensicsRepo = forensicsRepo;
        this.faceRepo = faceRepo;
        this.aiManipRepo = aiManipRepo;
        this.riskRepo = riskRepo;
        this.documentRepo = documentRepo;
        this.objectMapper = objectMapper;
    }

    /**
     * Run evidence fusion for the given task and document.
     * Persists a RiskAssessment and updates Document.riskLevel.
     */
    @Transactional
    public void fuse(AnalysisTask task, Document document) {
        UUID taskId = task.getId();
        try {
            List<String> explanations = new ArrayList<>();
            Map<String, Object> contributions = new LinkedHashMap<>();

            double weightedSum = 0.0;
            double totalWeight = 0.0;

            // --- Image Forensics ---
            var forensicsOpt = forensicsRepo.findByTaskId(taskId);
            if (forensicsOpt.isPresent()) {
                double s = forensicsScore(forensicsOpt.get(), explanations);
                weightedSum += s * W_FORENSICS;
                totalWeight += W_FORENSICS;
                contributions.put("forensics", round4(s));
            } else {
                contributions.put("forensics", null);
            }

            // --- AI Manipulation ---
            var aiOpt = aiManipRepo.findByTaskId(taskId);
            if (aiOpt.isPresent()) {
                double s = aiManipulationScore(aiOpt.get(), explanations);
                weightedSum += s * W_AI_MANIPULATION;
                totalWeight += W_AI_MANIPULATION;
                contributions.put("aiManipulation", round4(s));
            } else {
                contributions.put("aiManipulation", null);
            }

            // --- Metadata ---
            var metaOpt = metadataRepo.findByTaskId(taskId);
            if (metaOpt.isPresent()) {
                double s = metadataScore(metaOpt.get(), explanations);
                weightedSum += s * W_METADATA;
                totalWeight += W_METADATA;
                contributions.put("metadata", round4(s));
            } else {
                contributions.put("metadata", null);
            }

            // --- Identity Consistency ---
            var identOpt = identityRepo.findByTaskId(taskId);
            if (identOpt.isPresent()) {
                double s = identityScore(identOpt.get(), explanations);
                weightedSum += s * W_IDENTITY;
                totalWeight += W_IDENTITY;
                contributions.put("identity", round4(s));
            } else {
                contributions.put("identity", null);
            }

            // --- OCR ---
            var ocrOpt = ocrRepo.findByTaskId(taskId);
            if (ocrOpt.isPresent()) {
                double s = ocrScore(ocrOpt.get(), explanations);
                weightedSum += s * W_OCR;
                totalWeight += W_OCR;
                contributions.put("ocr", round4(s));
            } else {
                contributions.put("ocr", null);
            }

            // --- Face Verification ---
            var faceOpt = faceRepo.findByTaskId(taskId);
            if (faceOpt.isPresent()) {
                double s = faceScore(faceOpt.get(), explanations);
                // Only include weight if module ran (not NOT_APPLICABLE)
                String faceStatus = faceOpt.get().getStatus();
                if (!"NOT_APPLICABLE".equals(faceStatus)) {
                    weightedSum += s * W_FACE;
                    totalWeight += W_FACE;
                    contributions.put("face", round4(s));
                } else {
                    contributions.put("face", null); // NOT_APPLICABLE
                }
            } else {
                contributions.put("face", null);
            }

            // Compute final score
            double finalScore = totalWeight > 0 ? weightedSum / totalWeight : 0.0;
            finalScore = Math.min(1.0, Math.max(0.0, round4(finalScore)));
            String level = scoreToLevel(finalScore);

            // Level-specific summary explanation (prepended)
            String summary = switch (level) {
                case "HIGH"   -> "Overall risk assessment: HIGH. Multiple potential manipulation indicators detected.";
                case "MEDIUM" -> "Overall risk assessment: MEDIUM. Some potential manipulation indicators detected.";
                default       -> "Overall risk assessment: LOW. No significant manipulation indicators detected.";
            };
            explanations.add(0, summary);

            if (explanations.size() == 1) {
                explanations.add("No anomalies were detected by any analysis module.");
            }

            // Persist
            RiskAssessment ra = new RiskAssessment();
            ra.setTask(task);
            ra.setDocument(document);
            ra.setScore(BigDecimal.valueOf(finalScore));
            ra.setLevel(level);
            ra.setScoringVersion(SCORING_VERSION);

            try {
                ra.setExplanationJson(objectMapper.writeValueAsString(explanations));
                ra.setModuleContributionsJson(objectMapper.writeValueAsString(contributions));
            } catch (JsonProcessingException e) {
                log.warn("Could not serialise risk assessment JSON: {}", e.getMessage());
            }

            riskRepo.save(ra);

            // Update document riskLevel
            document.setRiskLevel(level);
            documentRepo.save(document);

            log.info("Evidence fusion completed for task {} — score: {}, level: {}", taskId, finalScore, level);

        } catch (Exception e) {
            log.error("Evidence fusion failed for task {}: {}", taskId, e.getMessage(), e);
        }
    }

    /**
     * Retrieve the latest risk assessment for a document (ownership check via userId).
     */
    @Transactional(readOnly = true)
    public RiskAssessmentResponse getRiskAssessment(UUID documentId, UUID userId) {
        documentRepo.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found or access denied"));

        RiskAssessment ra = riskRepo.findTopByDocumentIdOrderByCreatedAtDesc(documentId)
                .orElseThrow(() -> new RuntimeException("No risk assessment found for this document"));

        return RiskAssessmentResponse.fromEntity(ra);
    }

    // ---------------------------------------------------------------------------
    // Module score resolvers — each returns a normalized [0.0, 1.0] score
    // ---------------------------------------------------------------------------

    private double forensicsScore(ForensicResult r, List<String> explanations) {
        if ("SUCCESS".equals(r.getStatus()) && r.getRiskScore() != null) {
            double s = r.getRiskScore().doubleValue();
            if (s > 0.4) {
                explanations.add("Image forensics detected potential manipulation indicators " +
                        "(score: " + String.format("%.2f", s) + ").");
            }
            return s;
        }
        return 0.0;
    }

    private double aiManipulationScore(AiManipulationResult r, List<String> explanations) {
        if ("SUCCESS".equals(r.getStatus()) && r.getRiskScore() != null) {
            double s = r.getRiskScore().doubleValue();
            if (s > 0.4) {
                String type = r.getManipulationType() != null ? r.getManipulationType() : "UNKNOWN";
                explanations.add("AI manipulation screening returned elevated risk " +
                        "(score: " + String.format("%.2f", s) + ", type: " + type + ").");
            }
            return s;
        }
        return 0.0;
    }

    private double metadataScore(MetadataResult r, List<String> explanations) {
        if ("SUCCESS".equals(r.getStatus()) && r.getEvidenceScore() != null) {
            double s = r.getEvidenceScore().doubleValue();
            if (s > 0.15) {
                explanations.add("Metadata analysis found anomalies " +
                        "(evidence score: " + String.format("%.2f", s) + ").");
            }
            return s;
        }
        return 0.0;
    }

    private double identityScore(IdentityConsistencyResult r, List<String> explanations) {
        if ("SUCCESS".equals(r.getStatus())) {
            int fh = r.getFailedHigh()   != null ? r.getFailedHigh()   : 0;
            int fm = r.getFailedMedium() != null ? r.getFailedMedium() : 0;
            int fl = r.getFailedLow()    != null ? r.getFailedLow()    : 0;
            double s = Math.min(1.0, fh * 0.4 + fm * 0.15 + fl * 0.05);
            if (Boolean.FALSE.equals(r.getConsistent())) {
                explanations.add("Identity consistency checks found inconsistencies " +
                        "(high failures: " + fh + ", medium: " + fm + ", low: " + fl + ").");
            }
            return round4(s);
        }
        return 0.0;
    }

    private double ocrScore(OcrResult r, List<String> explanations) {
        if ("FAILED".equals(r.getStatus())) {
            explanations.add("OCR failed — text could not be extracted for verification.");
            return 0.1;
        }
        // OCR success/confidence is text-quality evidence, not a risk indicator itself
        return 0.0;
    }

    private double faceScore(FaceVerificationResult r, List<String> explanations) {
        String status = r.getStatus();
        if ("POSSIBLE_MISMATCH".equals(status)) {
            double sim = r.getSimilarity() != null ? r.getSimilarity().doubleValue() : 0.0;
            double thr = r.getThreshold()  != null ? r.getThreshold().doubleValue()  : 0.70;
            double s = Math.min(1.0, Math.max(0.0, (thr - sim) / thr));
            explanations.add("Face verification indicates a possible mismatch " +
                    "(similarity: " + String.format("%.2f", sim) + ", threshold: " + String.format("%.2f", thr) + ").");
            return round4(s);
        }
        if ("FAILED".equals(status)) {
            return 0.05;
        }
        return 0.0;
    }

    // ---------------------------------------------------------------------------
    // Utilities
    // ---------------------------------------------------------------------------

    private String scoreToLevel(double score) {
        if (score >= THRESHOLD_HIGH)   return "HIGH";
        if (score >= THRESHOLD_MEDIUM) return "MEDIUM";
        return "LOW";
    }

    private double round4(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
