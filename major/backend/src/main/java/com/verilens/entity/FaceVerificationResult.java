package com.verilens.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Persists the face verification module output for an analysis task.
 * Status values: POSSIBLE_MATCH | POSSIBLE_MISMATCH | NOT_APPLICABLE | FAILED
 * Implemented in Phase 8.
 */
@Entity
@Table(name = "face_verification_results")
public class FaceVerificationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private AnalysisTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "similarity", precision = 6, scale = 4)
    private BigDecimal similarity;

    @Column(name = "threshold", precision = 6, scale = 4)
    private BigDecimal threshold;

    @Column(name = "document_face_detected")
    private Boolean documentFaceDetected;

    @Column(name = "reference_face_detected")
    private Boolean referenceFaceDetected;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

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

    public FaceVerificationResult() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public AnalysisTask getTask() { return task; }
    public void setTask(AnalysisTask task) { this.task = task; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getSimilarity() { return similarity; }
    public void setSimilarity(BigDecimal similarity) { this.similarity = similarity; }

    public BigDecimal getThreshold() { return threshold; }
    public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }

    public Boolean getDocumentFaceDetected() { return documentFaceDetected; }
    public void setDocumentFaceDetected(Boolean documentFaceDetected) { this.documentFaceDetected = documentFaceDetected; }

    public Boolean getReferenceFaceDetected() { return referenceFaceDetected; }
    public void setReferenceFaceDetected(Boolean referenceFaceDetected) { this.referenceFaceDetected = referenceFaceDetected; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
