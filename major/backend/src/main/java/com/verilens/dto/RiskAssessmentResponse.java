package com.verilens.dto;

import com.verilens.entity.RiskAssessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Risk assessment response DTO — returned by GET /api/documents/{id}/analysis/risk.
 */
public class RiskAssessmentResponse {

    private UUID id;
    private BigDecimal score;
    private String level;
    private String explanationJson;
    private String moduleContributionsJson;
    private String scoringVersion;
    private Instant createdAt;

    public RiskAssessmentResponse() {}

    public static RiskAssessmentResponse fromEntity(RiskAssessment r) {
        RiskAssessmentResponse dto = new RiskAssessmentResponse();
        dto.setId(r.getId());
        dto.setScore(r.getScore());
        dto.setLevel(r.getLevel());
        dto.setExplanationJson(r.getExplanationJson());
        dto.setModuleContributionsJson(r.getModuleContributionsJson());
        dto.setScoringVersion(r.getScoringVersion());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getExplanationJson() { return explanationJson; }
    public void setExplanationJson(String explanationJson) { this.explanationJson = explanationJson; }

    public String getModuleContributionsJson() { return moduleContributionsJson; }
    public void setModuleContributionsJson(String moduleContributionsJson) { this.moduleContributionsJson = moduleContributionsJson; }

    public String getScoringVersion() { return scoringVersion; }
    public void setScoringVersion(String scoringVersion) { this.scoringVersion = scoringVersion; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
