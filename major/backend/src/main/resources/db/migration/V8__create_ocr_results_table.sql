-- =============================================================================
-- V8: Create ocr_results table
-- Phase 4 — OCR
-- =============================================================================

CREATE TABLE ocr_results (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id             UUID NOT NULL REFERENCES analysis_tasks(id) ON DELETE CASCADE,
    document_id         UUID NOT NULL REFERENCES documents(id)      ON DELETE CASCADE,
    status              VARCHAR(20)  NOT NULL,
    extracted_text      TEXT,
    words_json          TEXT,
    overall_confidence  DECIMAL(5, 4),
    page_count          INT,
    error_message       TEXT,
    processing_time_ms  BIGINT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ocr_results_task_id     ON ocr_results(task_id);
CREATE INDEX idx_ocr_results_document_id ON ocr_results(document_id);
