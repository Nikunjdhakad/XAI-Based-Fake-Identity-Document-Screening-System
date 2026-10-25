package com.verilens.dto;

import com.verilens.entity.FaceVerificationResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Face verification result DTO — returned by GET /api/documents/{id}/analysis/face.
 */
public class FaceVerificationResponse {

    private UUID id;
    private String status;
    private BigDecimal similarity;
    private BigDecimal threshold;
    private Boolean documentFaceDetected;
    private Boolean referenceFaceDetected;
    private String note;
    private String reason;
    private String errorMessage;
    private Long processingTimeMs;
    private Instant createdAt;

    public FaceVerificationResponse() {}

    public static FaceVerificationResponse fromEntity(FaceVerificationResult f) {
        FaceVerificationResponse dto = new FaceVerificationResponse();
        dto.setId(f.getId());
        dto.setStatus(f.getStatus());
        dto.setSimilarity(f.getSimilarity());
        dto.setThreshold(f.getThreshold());
        dto.setDocumentFaceDetected(f.getDocumentFaceDetected());
        dto.setReferenceFaceDetected(f.getReferenceFaceDetected());
        dto.setNote(f.getNote());
        dto.setReason(f.getReason());
        dto.setErrorMessage(f.getErrorMessage());
        dto.setProcessingTimeMs(f.getProcessingTimeMs());
        dto.setCreatedAt(f.getCreatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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
