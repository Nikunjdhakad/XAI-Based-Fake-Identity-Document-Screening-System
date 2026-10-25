package com.verilens.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * AI Manipulation screening result — Phase 9.
 *
 * Stores the output of frequency-domain and statistical analysis
 * used to screen for possible AI-assisted manipulation (inpainting,
 * splicing, generation).
 *
 * This is a screening signal — never a definitive proof.
 */
@Entity
@Table(name = "ai_manipulation_results")
public class AiManipulationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private AnalysisTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    /** Module status: SUCCESS, FAILED, NOT_APPLICABLE */
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    /** Weighted risk score from all techniques (0.0–1.0) */
    @Column(name = "risk_score", precision = 6, scale = 4)
    private BigDecimal riskScore;

    /** Inferred manipulation type: UNKNOWN, INPAINTING, SPLICING, GENERATION, NONE_DETECTED */
    @Column(name = "manipulation_type", length = 30)
    private String manipulationType;

    /** Overall confidence (0.0–1.0) */
    @Column(name = "confidence", precision = 6, scale = 4)
    private BigDecimal confidence;

    /** JSON array of technique findings */
    @Column(name = "findings_json", columnDefinition = "TEXT")
    private String findingsJson;

    /** JSON array of suspicious regions with bounding boxes */
    @Column(name = "suspicious_regions_json", columnDefinition = "TEXT")
    private String suspiciousRegionsJson;

    /** JSON object with model/technique information */
    @Column(name = "model_info_json", columnDefinition = "TEXT")
    private String modelInfoJson;

    /** Error message if status is FAILED */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public AnalysisTask getTask() { return task; }
    public void setTask(AnalysisTask task) { this.task = task; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }

    public String getManipulationType() { return manipulationType; }
    public void setManipulationType(String manipulationType) { this.manipulationType = manipulationType; }

    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }

    public String getFindingsJson() { return findingsJson; }
    public void setFindingsJson(String findingsJson) { this.findingsJson = findingsJson; }

    public String getSuspiciousRegionsJson() { return suspiciousRegionsJson; }
    public void setSuspiciousRegionsJson(String suspiciousRegionsJson) { this.suspiciousRegionsJson = suspiciousRegionsJson; }

    public String getModelInfoJson() { return modelInfoJson; }
    public void setModelInfoJson(String modelInfoJson) { this.modelInfoJson = modelInfoJson; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
