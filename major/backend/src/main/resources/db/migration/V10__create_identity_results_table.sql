-- =============================================================================
-- V10: Create identity_consistency_results table
-- Phase 6 — Identity Consistency
-- =============================================================================

CREATE TABLE identity_consistency_results (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id             UUID NOT NULL REFERENCES analysis_tasks(id) ON DELETE CASCADE,
    document_id         UUID NOT NULL REFERENCES documents(id)      ON DELETE CASCADE,
    status              VARCHAR(20)  NOT NULL,
    consistent          BOOLEAN,
    checks_json         TEXT,
    failed_high         INT,
    failed_medium       INT,
    failed_low          INT,
    error_message       TEXT,
    processing_time_ms  BIGINT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_identity_results_task_id     ON identity_consistency_results(task_id);
CREATE INDEX idx_identity_results_document_id ON identity_consistency_results(document_id);
