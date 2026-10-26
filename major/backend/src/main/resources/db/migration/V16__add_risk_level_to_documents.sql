-- =============================================================================
-- V14: Add risk_level column to documents table
-- Phase 10 — Evidence Fusion & Risk Scoring
-- =============================================================================

ALTER TABLE documents ADD COLUMN IF NOT EXISTS risk_level VARCHAR(10);
CREATE INDEX IF NOT EXISTS idx_documents_risk_level ON documents(risk_level);
