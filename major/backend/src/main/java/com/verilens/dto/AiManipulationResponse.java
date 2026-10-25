package com.verilens.dto;

import com.verilens.entity.AiManipulationResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for AI Manipulation screening result — Phase 9.
 * Safe response projection — never exposes internal entity relationships.
 */
public class AiManipulationResponse {

    private UUID id;
    private UUID taskId;
    private UUID documentId;
    private String status;
    private BigDecimal riskScore;
    private String manipulationType;
    private BigDecimal confidence;
    private String findingsJson;
    private String suspiciousRegionsJson;
    private String modelInfoJson;
    private String errorMessage;
    private Long processingTimeMs;
    private Instant createdAt;

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    public static AiManipulationResponse fromEntity(AiManipulationResult entity) {
        AiManipulationResponse dto = new AiManipulationResponse();
        dto.setId(entity.getId());
        dto.setTaskId(entity.getTask().getId());
        dto.setDocumentId(entity.getDocument().getId());
        dto.setStatus(entity.getStatus());
        dto.setRiskScore(entity.getRiskScore());
        dto.setManipulationType(entity.getManipulationType());
        dto.setConfidence(entity.getConfidence());
        dto.setFindingsJson(entity.getFindingsJson());
        dto.setSuspiciousRegionsJson(entity.getSuspiciousRegionsJson());
        dto.setModelInfoJson(entity.getModelInfoJson());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setProcessingTimeMs(entity.getProcessingTimeMs());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTaskId() { return taskId; }
    public void setTaskId(UUID taskId) { this.taskId = taskId; }

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }

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
