package com.verilens.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Persists the identity consistency module output for an analysis task.
 * checks_json stores the per-rule results as a JSON string.
 * Implemented in Phase 6.
 */
@Entity
@Table(name = "identity_consistency_results")
public class IdentityConsistencyResult {

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

    @Column(name = "consistent")
    private Boolean consistent;

    @Column(name = "checks_json", columnDefinition = "TEXT")
    private String checksJson;

    @Column(name = "failed_high")
    private Integer failedHigh;

    @Column(name = "failed_medium")
    private Integer failedMedium;

    @Column(name = "failed_low")
    private Integer failedLow;

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

    public IdentityConsistencyResult() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public AnalysisTask getTask() { return task; }
    public void setTask(AnalysisTask task) { this.task = task; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getConsistent() { return consistent; }
    public void setConsistent(Boolean consistent) { this.consistent = consistent; }

    public String getChecksJson() { return checksJson; }
    public void setChecksJson(String checksJson) { this.checksJson = checksJson; }

    public Integer getFailedHigh() { return failedHigh; }
    public void setFailedHigh(Integer failedHigh) { this.failedHigh = failedHigh; }

    public Integer getFailedMedium() { return failedMedium; }
    public void setFailedMedium(Integer failedMedium) { this.failedMedium = failedMedium; }

    public Integer getFailedLow() { return failedLow; }
    public void setFailedLow(Integer failedLow) { this.failedLow = failedLow; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
