-- =============================================================================
-- V5: Create analysis_tasks table
-- =============================================================================

CREATE TABLE analysis_tasks (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id         UUID         NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    status              VARCHAR(30)  NOT NULL DEFAULT 'QUEUED',
    requested_modules   TEXT,
    started_at          TIMESTAMP,
    completed_at        TIMESTAMP,
    processing_time_ms  BIGINT,
    error_message       TEXT,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_analysis_tasks_document_id ON analysis_tasks(document_id);
CREATE INDEX idx_analysis_tasks_status ON analysis_tasks(status);
