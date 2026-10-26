-- =============================================================================
-- V13: Create risk_assessments table
-- Phase 10 — Evidence Fusion & Risk Scoring
-- =============================================================================

CREATE TABLE risk_assessments (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id                     UUID NOT NULL REFERENCES analysis_tasks(id) ON DELETE CASCADE,
    document_id                 UUID NOT NULL REFERENCES documents(id)      ON DELETE CASCADE,
    score                       DECIMAL(5, 4) NOT NULL,
    level                       VARCHAR(10)   NOT NULL,
    explanation_json            TEXT,
    module_contributions_json   TEXT,
    scoring_version             VARCHAR(20)   NOT NULL DEFAULT '1.0.0',
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_risk_assessments_task_id ON risk_assessments(task_id);
CREATE INDEX idx_risk_assessments_document_id ON risk_assessments(document_id);
