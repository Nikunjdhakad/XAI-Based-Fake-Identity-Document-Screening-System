-- =============================================================================
-- V12: Create face_verification_results table
-- Phase 8 — Face Verification
-- =============================================================================

CREATE TABLE face_verification_results (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id                 UUID NOT NULL REFERENCES analysis_tasks(id) ON DELETE CASCADE,
    document_id             UUID NOT NULL REFERENCES documents(id)      ON DELETE CASCADE,
    status                  VARCHAR(30)  NOT NULL,
    similarity              DECIMAL(6, 4),
    threshold               DECIMAL(6, 4),
    document_face_detected  BOOLEAN,
    reference_face_detected BOOLEAN,
    note                    TEXT,
    reason                  TEXT,
    error_message           TEXT,
    processing_time_ms      BIGINT,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_face_results_task_id     ON face_verification_results(task_id);
CREATE INDEX idx_face_results_document_id ON face_verification_results(document_id);
