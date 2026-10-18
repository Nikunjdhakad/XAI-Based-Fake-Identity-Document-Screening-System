-- =============================================================================
-- V7: Seed default roles
-- =============================================================================

INSERT INTO roles (id, name, description) VALUES
    (gen_random_uuid(), 'USER', 'Standard user — can upload documents and view own results'),
    (gen_random_uuid(), 'ANALYST', 'Analyst — can review all documents and analysis results'),
    (gen_random_uuid(), 'ADMIN', 'Administrator — full system access');
