package com.verilens.dto;

import com.verilens.entity.ForensicResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Forensic result DTO — returned by GET /api/documents/{id}/analysis/forensics.
 */
public class ForensicResultResponse {

    private UUID id;
    private String status;
    private BigDecimal riskScore;
    private String findingsJson;
    private String suspiciousRegionsJson;
    private BigDecimal elaMeanError;
    private BigDecimal noiseMean;
    private BigDecimal noiseStd;
    private BigDecimal edgeMean;
    private BigDecimal edgeStd;
    private String errorMessage;
    private Long processingTimeMs;
    private Instant createdAt;

    public ForensicResultResponse() {}

    public static ForensicResultResponse fromEntity(ForensicResult f) {
        ForensicResultResponse dto = new ForensicResultResponse();
        dto.setId(f.getId());
        dto.setStatus(f.getStatus());
        dto.setRiskScore(f.getRiskScore());
        dto.setFindingsJson(f.getFindingsJson());
        dto.setSuspiciousRegionsJson(f.getSuspiciousRegionsJson());
        dto.setElaMeanError(f.getElaMeanError());
        dto.setNoiseMean(f.getNoiseMean());
        dto.setNoiseStd(f.getNoiseStd());
        dto.setEdgeMean(f.getEdgeMean());
        dto.setEdgeStd(f.getEdgeStd());
        dto.setErrorMessage(f.getErrorMessage());
        dto.setProcessingTimeMs(f.getProcessingTimeMs());
        dto.setCreatedAt(f.getCreatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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
