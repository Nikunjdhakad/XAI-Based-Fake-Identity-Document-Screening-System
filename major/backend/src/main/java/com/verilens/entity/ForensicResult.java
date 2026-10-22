package com.verilens.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Persists the image forensics module output for an analysis task.
 * findings_json and suspicious_regions_json store structured data as JSON strings.
 * Implemented in Phase 7.
 */
@Entity
@Table(name = "forensic_results")
public class ForensicResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private AnalysisTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "risk_score", precision = 5, scale = 4)
    private BigDecimal riskScore;

    @Column(name = "findings_json", columnDefinition = "TEXT")
    private String findingsJson;

    @Column(name = "suspicious_regions_json", columnDefinition = "TEXT")
    private String suspiciousRegionsJson;

    @Column(name = "ela_mean_error", precision = 8, scale = 4)
    private BigDecimal elaMeanError;

    @Column(name = "noise_mean", precision = 8, scale = 4)
    private BigDecimal noiseMean;

    @Column(name = "noise_std", precision = 8, scale = 4)
    private BigDecimal noiseStd;

    @Column(name = "edge_mean", precision = 8, scale = 4)
    private BigDecimal edgeMean;

    @Column(name = "edge_std", precision = 8, scale = 4)
    private BigDecimal edgeStd;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public ForensicResult() {}

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

    public String getFindingsJson() { return findingsJson; }
    public void setFindingsJson(String findingsJson) { this.findingsJson = findingsJson; }

    public String getSuspiciousRegionsJson() { return suspiciousRegionsJson; }
    public void setSuspiciousRegionsJson(String suspiciousRegionsJson) { this.suspiciousRegionsJson = suspiciousRegionsJson; }

    public BigDecimal getElaMeanError() { return elaMeanError; }
    public void setElaMeanError(BigDecimal elaMeanError) { this.elaMeanError = elaMeanError; }

    public BigDecimal getNoiseMean() { return noiseMean; }
    public void setNoiseMean(BigDecimal noiseMean) { this.noiseMean = noiseMean; }

    public BigDecimal getNoiseStd() { return noiseStd; }
    public void setNoiseStd(BigDecimal noiseStd) { this.noiseStd = noiseStd; }

    public BigDecimal getEdgeMean() { return edgeMean; }
    public void setEdgeMean(BigDecimal edgeMean) { this.edgeMean = edgeMean; }

    public BigDecimal getEdgeStd() { return edgeStd; }
    public void setEdgeStd(BigDecimal edgeStd) { this.edgeStd = edgeStd; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
