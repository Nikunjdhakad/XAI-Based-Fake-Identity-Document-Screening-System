package com.verilens.dto;

import com.verilens.entity.IdentityConsistencyResult;

import java.time.Instant;
import java.util.UUID;

/**
 * Identity consistency result DTO — returned by GET /api/documents/{id}/analysis/identity.
 */
public class IdentityConsistencyResponse {

    private UUID id;
    private String status;
    private Boolean consistent;
    private String checksJson;
    private Integer failedHigh;
    private Integer failedMedium;
    private Integer failedLow;
    private String errorMessage;
    private Long processingTimeMs;
    private Instant createdAt;

    public IdentityConsistencyResponse() {}

    public static IdentityConsistencyResponse fromEntity(IdentityConsistencyResult r) {
        IdentityConsistencyResponse dto = new IdentityConsistencyResponse();
        dto.setId(r.getId());
        dto.setStatus(r.getStatus());
        dto.setConsistent(r.getConsistent());
        dto.setChecksJson(r.getChecksJson());
        dto.setFailedHigh(r.getFailedHigh());
        dto.setFailedMedium(r.getFailedMedium());
        dto.setFailedLow(r.getFailedLow());
        dto.setErrorMessage(r.getErrorMessage());
        dto.setProcessingTimeMs(r.getProcessingTimeMs());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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
