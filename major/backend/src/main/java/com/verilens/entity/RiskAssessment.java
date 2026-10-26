package com.verilens.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Persists the final risk assessment produced by evidence fusion.
 * scoring_version tracks the fusion configuration for auditability.
 * Implemented in Phase 10.
 */
@Entity
@Table(name = "risk_assessments")
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private AnalysisTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal score;

    @Column(nullable = false, length = 10)
    private String level;

    @Column(name = "explanation_json", columnDefinition = "TEXT")
    private String explanationJson;

    @Column(name = "module_contributions_json", columnDefinition = "TEXT")
    private String moduleContributionsJson;

    @Column(name = "scoring_version", nullable = false, length = 20)
    private String scoringVersion = "1.0.0";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() { createdAt = Instant.now(); }

    public RiskAssessment() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public AnalysisTask getTask() { return task; }
    public void setTask(AnalysisTask task) { this.task = task; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getExplanationJson() { return explanationJson; }
    public void setExplanationJson(String explanationJson) { this.explanationJson = explanationJson; }

    public String getModuleContributionsJson() { return moduleContributionsJson; }
    public void setModuleContributionsJson(String json) { this.moduleContributionsJson = json; }

    public String getScoringVersion() { return scoringVersion; }
    public void setScoringVersion(String scoringVersion) { this.scoringVersion = scoringVersion; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
