-- =============================================================================
-- V4: Create document_files table
-- =============================================================================

CREATE TABLE document_files (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id       UUID         NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    file_type         VARCHAR(30)  NOT NULL,
    storage_path      VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    mime_type         VARCHAR(100) NOT NULL,
    file_size         BIGINT       NOT NULL,
    checksum          VARCHAR(64),
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_document_files_document_id ON document_files(document_id);
