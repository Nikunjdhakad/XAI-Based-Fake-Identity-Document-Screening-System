-- =============================================================================
-- V13: AI Manipulation Results table — Phase 9
-- =============================================================================
-- Stores results from the AI manipulation screening module.
-- Each row links to one analysis_task and one document.
-- =============================================================================

CREATE TABLE ai_manipulation_results (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id                 UUID NOT NULL REFERENCES analysis_tasks(id) ON DELETE CASCADE,
    document_id             UUID NOT NULL REFERENCES documents(id)      ON DELETE CASCADE,
    status                  VARCHAR(30) NOT NULL,
    risk_score              DECIMAL(6, 4),
    manipulation_type       VARCHAR(30),
    confidence              DECIMAL(6, 4),
    findings_json           TEXT,
    suspicious_regions_json TEXT,
    model_info_json         TEXT,
    error_message           TEXT,
    processing_time_ms      BIGINT,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_manip_results_task_id     ON ai_manipulation_results(task_id);
CREATE INDEX idx_ai_manip_results_document_id ON ai_manipulation_results(document_id);
