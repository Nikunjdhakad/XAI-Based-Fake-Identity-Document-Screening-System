-- =============================================================================
-- V17: Create reports table
-- Phase 12 — Reports
-- =============================================================================

CREATE TABLE reports (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id   UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    task_id       UUID REFERENCES analysis_tasks(id) ON DELETE CASCADE,
    storage_path  VARCHAR(500) NOT NULL,
    format        VARCHAR(10) NOT NULL DEFAULT 'PDF',
    file_size     BIGINT,
    generated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reports_document_id ON reports(document_id);
CREATE INDEX idx_reports_task_id ON reports(task_id);
