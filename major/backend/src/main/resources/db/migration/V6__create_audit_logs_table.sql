-- =============================================================================
-- V6: Create audit_logs table
-- =============================================================================

CREATE TABLE audit_logs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         REFERENCES users(id),
    action       VARCHAR(50)  NOT NULL,
    entity_type  VARCHAR(50),
    entity_id    UUID,
    details      TEXT,
    ip_address   VARCHAR(45),
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
