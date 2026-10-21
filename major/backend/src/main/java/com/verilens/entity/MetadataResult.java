package com.verilens.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Persists the metadata analysis module output for an analysis task.
 * properties_json and anomalies_json store structured data as JSON strings.
 * Implemented in Phase 5.
 */
@Entity
@Table(name = "metadata_results")
public class MetadataResult {

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

    @Column(name = "available")
    private Boolean available;

    @Column(name = "properties_json", columnDefinition = "TEXT")
    private String propertiesJson;

    @Column(name = "anomalies_json", columnDefinition = "TEXT")
    private String anomaliesJson;

    @Column(name = "evidence_score", precision = 5, scale = 4)
    private BigDecimal evidenceScore;

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

    public MetadataResult() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public AnalysisTask getTask() { return task; }
    public void setTask(AnalysisTask task) { this.task = task; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }

    public String getPropertiesJson() { return propertiesJson; }
    public void setPropertiesJson(String propertiesJson) { this.propertiesJson = propertiesJson; }

    public String getAnomaliesJson() { return anomaliesJson; }
    public void setAnomaliesJson(String anomaliesJson) { this.anomaliesJson = anomaliesJson; }

    public BigDecimal getEvidenceScore() { return evidenceScore; }
    public void setEvidenceScore(BigDecimal evidenceScore) { this.evidenceScore = evidenceScore; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
