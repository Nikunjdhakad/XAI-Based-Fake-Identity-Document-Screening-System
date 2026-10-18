-- =============================================================================
-- V3: Create documents table
-- =============================================================================

CREATE TABLE documents (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID         NOT NULL REFERENCES users(id),
    original_filename VARCHAR(255) NOT NULL,
    document_type     VARCHAR(50)  NOT NULL,
    description       TEXT,
    status            VARCHAR(30)  NOT NULL DEFAULT 'UPLOADED',
    mime_type         VARCHAR(100) NOT NULL,
    file_size         BIGINT       NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_documents_user_id ON documents(user_id);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_created_at ON documents(created_at);
