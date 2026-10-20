# VeriLens — Database Design

**Version:** 0.1.0  
**Phase:** 2 — Backend Foundation  
**Last updated:** 2026-10-20

---

## 1. Overview

PostgreSQL 16 is the sole relational database. Only the Spring Boot backend accesses it directly via Spring Data JPA.

---

## 2. Design Principles

- **UUIDs** for all public-facing identifiers
- **Timestamps** (`created_at`, `updated_at`) on all mutable entities
- **Foreign keys** to enforce referential integrity — no data duplication
- **bcrypt** for password hashing — never plaintext
- **Soft delete** considered where audit trail requires it
- **Indexing** on frequently queried columns (user_id, document_id, status)

---

## 3. Entity Relationship Diagram

```text
┌────────┐       ┌──────┐
│  User  │──────▶│ Role │
└───┬────┘       └──────┘
    │ 1
    │
    │ *
┌───┴────────┐
│  Document  │
└───┬────────┘
    │ 1
    │
    ├──────────────────┐
    │ 1                │ 1
┌───┴──────────┐  ┌────┴──────────┐
│ DocumentFile │  │ AnalysisJob   │
└──────────────┘  └───┬───────────┘
                      │ 1
                      │
        ┌─────────────┼─────────────────────────────────┐
        │             │             │                     │
   ┌────┴────┐  ┌─────┴──────┐ ┌───┴────────┐     ┌─────┴──────────┐
   │OcrResult│  │ForensicRes.│ │MetadataRes.│     │IdentityConsist.│
   └─────────┘  └────────────┘ └────────────┘     └────────────────┘
        │             │
        │        ┌────┴──────────┐  ┌────────────────┐
        │        │FaceVerif.Res. │  │AiManipulation  │
        │        └───────────────┘  └────────────────┘
        │
   ┌────┴────────┐
   │  Evidence   │
   └────┬────────┘
        │ *
   ┌────┴──────────────┐
   │ SuspiciousRegion   │
   └───────────────────┘

   ┌────────────────┐
   │ RiskAssessment │──▶ Document
   └────────────────┘

   ┌────────┐
   │ Report │──▶ Document
   └────────┘

   ┌──────────┐
   │ AuditLog │──▶ User
   └──────────┘
```

---

## 4. Entity Definitions

### 4.1 User

| Column       | Type         | Constraints             |
| ------------ | ------------ | ----------------------- |
| id           | UUID         | PK                      |
| username     | VARCHAR(50)  | UNIQUE, NOT NULL        |
| email        | VARCHAR(255) | UNIQUE, NOT NULL        |
| password_hash| VARCHAR(255) | NOT NULL                |
| full_name    | VARCHAR(100) | NOT NULL                |
| role_id      | UUID         | FK → Role               |
| enabled      | BOOLEAN      | DEFAULT true            |
| created_at   | TIMESTAMP    | NOT NULL                |
| updated_at   | TIMESTAMP    | NOT NULL                |

### 4.2 Role

| Column       | Type         | Constraints             |
| ------------ | ------------ | ----------------------- |
| id           | UUID         | PK                      |
| name         | VARCHAR(30)  | UNIQUE, NOT NULL        |
| description  | VARCHAR(255) |                         |

Default roles: `ADMIN`, `ANALYST`, `USER`

### 4.3 Document

| Column         | Type         | Constraints             |
| -------------- | ------------ | ----------------------- |
| id             | UUID         | PK                      |
| user_id        | UUID         | FK → User, NOT NULL     |
| original_filename | VARCHAR(255) | NOT NULL             |
| document_type  | VARCHAR(50)  | NOT NULL                |
| description    | TEXT         |                         |
| status         | VARCHAR(30)  | NOT NULL                |
| mime_type      | VARCHAR(100) | NOT NULL                |
| file_size      | BIGINT       | NOT NULL                |
| created_at     | TIMESTAMP    | NOT NULL                |
| updated_at     | TIMESTAMP    | NOT NULL                |

Status values: `UPLOADED`, `QUEUED`, `PROCESSING`, `COMPLETED`, `FAILED`, `PARTIALLY_COMPLETED`

### 4.4 DocumentFile

| Column           | Type         | Constraints             |
| ---------------- | ------------ | ----------------------- |
| id               | UUID         | PK                      |
| document_id      | UUID         | FK → Document, NOT NULL |
| file_type        | VARCHAR(30)  | NOT NULL (DOCUMENT, REFERENCE_FACE) |
| storage_path     | VARCHAR(500) | NOT NULL                |
| original_filename| VARCHAR(255) | NOT NULL                |
| mime_type        | VARCHAR(100) | NOT NULL                |
| file_size        | BIGINT       | NOT NULL                |
| checksum         | VARCHAR(64)  |                         |
| created_at       | TIMESTAMP    | NOT NULL                |

### 4.5 AnalysisJob

| Column         | Type         | Constraints             |
| -------------- | ------------ | ----------------------- |
| id             | UUID         | PK                      |
| document_id    | UUID         | FK → Document, NOT NULL |
| status         | VARCHAR(30)  | NOT NULL                |
| requested_modules | TEXT      |                         |
| started_at     | TIMESTAMP    |                         |
| completed_at   | TIMESTAMP    |                         |
| processing_time_ms | BIGINT  |                         |
| error_message  | TEXT         |                         |
| created_at     | TIMESTAMP    | NOT NULL                |
| updated_at     | TIMESTAMP    | NOT NULL                |

### 4.6 OcrResult

| Column         | Type         | Constraints             |
| -------------- | ------------ | ----------------------- |
| id             | UUID         | PK                      |
| job_id         | UUID         | FK → AnalysisJob        |
| status         | VARCHAR(20)  | NOT NULL                |
| extracted_text | TEXT         |                         |
| fields_json    | JSONB        |                         |
| overall_confidence | DECIMAL  |                         |
| processing_time_ms | BIGINT  |                         |
| created_at     | TIMESTAMP    | NOT NULL                |

### 4.7 ForensicResult

| Column         | Type         | Constraints             |
| -------------- | ------------ | ----------------------- |
| id             | UUID         | PK                      |
| job_id         | UUID         | FK → AnalysisJob        |
| status         | VARCHAR(20)  | NOT NULL                |
| risk_score     | DECIMAL      |                         |
| findings_json  | JSONB        |                         |
| processing_time_ms | BIGINT  |                         |
| created_at     | TIMESTAMP    | NOT NULL                |

### 4.8 MetadataResult

| Column         | Type         | Constraints             |
| -------------- | ------------ | ----------------------- |
| id             | UUID         | PK                      |
| job_id         | UUID         | FK → AnalysisJob        |
| status         | VARCHAR(20)  | NOT NULL                |
| available      | BOOLEAN      |                         |
| properties_json| JSONB        |                         |
| anomalies_json | JSONB        |                         |
| evidence_score | DECIMAL      |                         |
| processing_time_ms | BIGINT  |                         |
| created_at     | TIMESTAMP    | NOT NULL                |

### 4.9 IdentityConsistencyResult

| Column         | Type         | Constraints             |
| -------------- | ------------ | ----------------------- |
| id             | UUID         | PK                      |
| job_id         | UUID         | FK → AnalysisJob        |
| status         | VARCHAR(20)  | NOT NULL                |
| consistent     | BOOLEAN      |                         |
| checks_json    | JSONB        |                         |
| processing_time_ms | BIGINT  |                         |
| created_at     | TIMESTAMP    | NOT NULL                |

### 4.10 FaceVerificationResult

| Column         | Type         | Constraints             |
| -------------- | ------------ | ----------------------- |
| id             | UUID         | PK                      |
| job_id         | UUID         | FK → AnalysisJob        |
| status         | VARCHAR(20)  | NOT NULL                |
| match_status   | VARCHAR(20)  |                         |
| similarity     | DECIMAL      |                         |
| processing_time_ms | BIGINT  |                         |
| created_at     | TIMESTAMP    | NOT NULL                |

Match status values: `MATCH`, `MISMATCH`, `NOT_APPLICABLE`

### 4.11 AiManipulationResult

| Column         | Type         | Constraints             |
| -------------- | ------------ | ----------------------- |
| id             | UUID         | PK                      |
| job_id         | UUID         | FK → AnalysisJob        |
| status         | VARCHAR(20)  | NOT NULL                |
| risk_score     | DECIMAL      |                         |
| manipulation_type | VARCHAR(50) |                       |
| confidence     | DECIMAL      |                         |
| processing_time_ms | BIGINT  |                         |
| created_at     | TIMESTAMP    | NOT NULL                |

### 4.12 Evidence

| Column         | Type         | Constraints             |
| -------------- | ------------ | ----------------------- |
| id             | UUID         | PK                      |
| job_id         | UUID         | FK → AnalysisJob        |
| module         | VARCHAR(30)  | NOT NULL                |
| evidence_type  | VARCHAR(50)  | NOT NULL                |
| severity       | VARCHAR(20)  | NOT NULL                |
| description    | TEXT         | NOT NULL                |
| details_json   | JSONB        |                         |
| created_at     | TIMESTAMP    | NOT NULL                |

### 4.13 SuspiciousRegion

| Column         | Type         | Constraints             |
| -------------- | ------------ | ----------------------- |
| id             | UUID         | PK                      |
| job_id         | UUID         | FK → AnalysisJob        |
| evidence_id    | UUID         | FK → Evidence           |
| source_module  | VARCHAR(30)  | NOT NULL                |
| x              | INT          | NOT NULL                |
| y              | INT          | NOT NULL                |
| width          | INT          | NOT NULL                |
| height         | INT          | NOT NULL                |
| confidence     | DECIMAL      |                         |
| description    | TEXT         |                         |
| created_at     | TIMESTAMP    | NOT NULL                |

### 4.14 RiskAssessment

| Column               | Type         | Constraints             |
| -------------------- | ------------ | ----------------------- |
| id                   | UUID         | PK                      |
| document_id          | UUID         | FK → Document, UNIQUE   |
| job_id               | UUID         | FK → AnalysisJob        |
| score                | DECIMAL      | NOT NULL                |
| level                | VARCHAR(10)  | NOT NULL                |
| explanation_json     | JSONB        |                         |
| module_contributions_json | JSONB   |                         |
| scoring_version      | VARCHAR(20)  | NOT NULL                |
| created_at           | TIMESTAMP    | NOT NULL                |

### 4.15 Report

| Column         | Type         | Constraints             |
| -------------- | ------------ | ----------------------- |
| id             | UUID         | PK                      |
| document_id    | UUID         | FK → Document           |
| job_id         | UUID         | FK → AnalysisJob        |
| storage_path   | VARCHAR(500) |                         |
| format         | VARCHAR(10)  | NOT NULL (PDF)          |
| generated_at   | TIMESTAMP    | NOT NULL                |

### 4.16 AuditLog

| Column         | Type         | Constraints             |
| -------------- | ------------ | ----------------------- |
| id             | UUID         | PK                      |
| user_id        | UUID         | FK → User (nullable)    |
| action         | VARCHAR(50)  | NOT NULL                |
| entity_type    | VARCHAR(50)  |                         |
| entity_id      | UUID         |                         |
| details_json   | JSONB        |                         |
| ip_address     | VARCHAR(45)  |                         |
| created_at     | TIMESTAMP    | NOT NULL                |

---

## 5. Indexes (Planned)

| Table          | Column(s)           | Type     |
| -------------- | ------------------- | -------- |
| document       | user_id             | btree    |
| document       | status              | btree    |
| document       | created_at          | btree    |
| analysis_job   | document_id         | btree    |
| analysis_job   | status              | btree    |
| evidence       | job_id              | btree    |
| suspicious_region | job_id           | btree    |
| risk_assessment| document_id         | unique   |
| audit_log      | user_id             | btree    |
| audit_log      | created_at          | btree    |
| audit_log      | action              | btree    |

---

## 6. Implementation Notes

- **Tables 1–7 (roles, users, documents, document_files, analysis_tasks, audit_logs) are now implemented** via Flyway migrations V1–V7 (see `backend/src/main/resources/db/migration/`). The entities are mapped via JPA with `ddl-auto: validate` in production.
- Tests use H2 in-memory with `MODE=PostgreSQL` and Hibernate `ddl-auto: create-drop` (Flyway disabled). See [decisions.md](decisions.md) DEC-009 for rationale.
- JSONB columns are used for semi-structured module output that varies per module
- All result tables link back to `AnalysisJob` for traceability
- The schema will be refined as each module is implemented (Phases 4–9)
- Remaining tables (OcrResult, ForensicResult, MetadataResult, IdentityConsistencyResult, FaceVerificationResult, AiManipulationResult, Evidence, SuspiciousRegion, RiskAssessment, Report) will be created in later phases
