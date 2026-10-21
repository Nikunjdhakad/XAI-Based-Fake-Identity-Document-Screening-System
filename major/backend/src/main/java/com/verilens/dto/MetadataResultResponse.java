package com.verilens.dto;

import com.verilens.entity.MetadataResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Metadata result DTO — returned by GET /api/documents/{id}/analysis/metadata.
 */
public class MetadataResultResponse {

    private UUID id;
    private String status;
    private Boolean available;
    private String propertiesJson;
    private String anomaliesJson;
    private BigDecimal evidenceScore;
    private String errorMessage;
    private Long processingTimeMs;
    private Instant createdAt;

    public MetadataResultResponse() {}

    public static MetadataResultResponse fromEntity(MetadataResult m) {
        MetadataResultResponse dto = new MetadataResultResponse();
        dto.setId(m.getId());
        dto.setStatus(m.getStatus());
        dto.setAvailable(m.getAvailable());
        dto.setPropertiesJson(m.getPropertiesJson());
        dto.setAnomaliesJson(m.getAnomaliesJson());
        dto.setEvidenceScore(m.getEvidenceScore());
        dto.setErrorMessage(m.getErrorMessage());
        dto.setProcessingTimeMs(m.getProcessingTimeMs());
        dto.setCreatedAt(m.getCreatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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
