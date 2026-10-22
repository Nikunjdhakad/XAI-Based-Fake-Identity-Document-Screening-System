-- =============================================================================
-- V11: Create forensic_results table
-- Phase 7 — Image Forensics
-- =============================================================================

CREATE TABLE forensic_results (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id               UUID NOT NULL REFERENCES analysis_tasks(id) ON DELETE CASCADE,
    document_id           UUID NOT NULL REFERENCES documents(id)      ON DELETE CASCADE,
    status                VARCHAR(20)  NOT NULL,
    risk_score            DECIMAL(5, 4),
    findings_json         TEXT,
    suspicious_regions_json TEXT,
    ela_mean_error        DECIMAL(8, 4),
    noise_mean             DECIMAL(8, 4),
    noise_std              DECIMAL(8, 4),
    edge_mean              DECIMAL(8, 4),
    edge_std               DECIMAL(8, 4),
    error_message          TEXT,
    processing_time_ms     BIGINT,
    created_at             TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_forensic_results_task_id     ON forensic_results(task_id);
CREATE INDEX idx_forensic_results_document_id ON forensic_results(document_id);
