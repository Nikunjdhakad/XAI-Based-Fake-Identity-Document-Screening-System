-- =============================================================================
-- V1: Create roles table
-- =============================================================================

CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(30) NOT NULL UNIQUE,
    description VARCHAR(255)
);
