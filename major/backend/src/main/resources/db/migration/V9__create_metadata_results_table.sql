-- =============================================================================
-- V9: Create metadata_results table
-- Phase 5 — Metadata Analysis
-- =============================================================================

CREATE TABLE metadata_results (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id             UUID NOT NULL REFERENCES analysis_tasks(id) ON DELETE CASCADE,
    document_id         UUID NOT NULL REFERENCES documents(id)      ON DELETE CASCADE,
    status              VARCHAR(20)  NOT NULL,
    available           BOOLEAN,
    properties_json     TEXT,
    anomalies_json      TEXT,
    evidence_score      DECIMAL(5, 4),
    error_message       TEXT,
    processing_time_ms  BIGINT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_metadata_results_task_id     ON metadata_results(task_id);
CREATE INDEX idx_metadata_results_document_id ON metadata_results(document_id);
