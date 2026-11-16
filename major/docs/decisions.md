# VeriLens — Decision Log

**Last updated:** 2026-10-20

---

This document records architectural and technical decisions made during development. Each decision includes context, the decision itself, and rationale.

---

## Decision Format

```
### DEC-NNN: Title
**Date:** YYYY-MM-DD
**Phase:** N
**Status:** ACCEPTED | SUPERSEDED | PENDING

**Context:** Why this decision was needed.

**Decision:** What was decided.

**Rationale:** Why this option was chosen.

**Alternatives considered:** What else was evaluated.

**Consequences:** What changes or constraints follow from this decision.
```

---

## Decisions

### DEC-001: Monorepo Structure

**Date:** 2026-10-07  
**Phase:** 0  
**Status:** ACCEPTED

**Context:** The project has three services (React frontend, Spring Boot backend, Python AI service) plus shared documentation, Docker configuration, and database schemas.

**Decision:** Use a single monorepo containing all services under `frontend/`, `backend/`, `ai-service/`, `database/`, and `docs/`.

**Rationale:** Simplifies development for a single-team academic project. Keeps documentation, Docker Compose, and environment configuration co-located. Avoids cross-repo coordination overhead.

**Alternatives considered:**
- Separate repositories per service — rejected due to unnecessary complexity for a single-developer project.

**Consequences:** All services share a single Git history. CI will need to handle selective builds.

---

### DEC-002: Spring Boot as Central Orchestrator

**Date:** 2026-10-07  
**Phase:** 0  
**Status:** ACCEPTED

**Context:** The project must demonstrate strong Java Spring Boot development. AI/CV capabilities require Python.

**Decision:** Spring Boot is the central application backend, owning all business logic, data access, orchestration, authentication, and API surface. Python is a subordinate internal service handling only AI/CV inference.

**Rationale:** The primary technical objective is demonstrating Java Spring Boot expertise. Python is necessary only for OCR, computer vision, and ML inference, which are impractical in Java.

**Alternatives considered:**
- Python as the main backend with Spring Boot as a secondary service — rejected; contradicts the primary project objective.
- Single Java monolith with JNI/GraalPy for Python — rejected; too fragile and complex.

**Consequences:** The frontend communicates only with Spring Boot. Spring Boot calls Python internally. Python never accesses PostgreSQL directly.

---

### DEC-003: JWT Authentication

**Date:** 2026-10-07  
**Phase:** 0  
**Status:** ACCEPTED

**Context:** The application needs stateless authentication suitable for a REST API architecture.

**Decision:** Use JWT tokens issued by Spring Security, sent in the `Authorization: Bearer` header.

**Rationale:** Stateless authentication aligns with the REST API design. Avoids server-side session management. Well-supported by Spring Security.

**Alternatives considered:**
- Session-based authentication — rejected; requires server-side state, less suitable for API-first design.
- OAuth2/OIDC — rejected; unnecessary complexity for an academic project with its own user base.

**Consequences:** Token expiration, refresh, and secret management must be handled.

---

### DEC-004: PostgreSQL as Sole Database

**Date:** 2026-10-07  
**Phase:** 0  
**Status:** ACCEPTED

**Context:** The application needs persistent storage for users, documents, analysis results, evidence, and audit trails.

**Decision:** Use PostgreSQL 16 as the sole database, accessed only by Spring Boot via JPA.

**Rationale:** The domain is primarily relational (users → documents → analysis jobs → results). PostgreSQL supports JSONB for semi-structured module output. Mature, well-supported, free.

**Alternatives considered:**
- MongoDB — rejected; the data model is fundamentally relational.
- SQLite — rejected; insufficient for multi-service architecture.
- PostgreSQL + Redis — Redis may be added later for caching if performance demands it, but is not needed initially.

**Consequences:** All persistent data goes through Spring Boot → PostgreSQL. JSONB is used for variable-structure module output.

---

### DEC-005: Internal REST for Backend ↔ AI Service

**Date:** 2026-10-07  
**Phase:** 0  
**Status:** ACCEPTED

**Context:** Spring Boot needs to communicate with the Python AI service.

**Decision:** Use internal REST/JSON with API key authentication.

**Rationale:** Simple, sufficient for the project's scale. Both frameworks have excellent HTTP support. Debugging is straightforward with JSON.

**Alternatives considered:**
- gRPC — rejected; adds protobuf dependency and complexity without clear benefit at this scale.
- Message queue (RabbitMQ/Kafka) — may be considered later if async processing is needed, but REST is simpler for now.

**Consequences:** Synchronous HTTP calls mean analysis latency includes network round-trip. For Phase 2+, consider async patterns if analysis takes >30 seconds.

---

### DEC-006: Docker Compose for Local Development

**Date:** 2026-10-07  
**Phase:** 0  
**Status:** ACCEPTED

**Context:** The project has four services (frontend, backend, AI service, database) that need to run together locally.

**Decision:** Use Docker Compose for local multi-service orchestration.

**Rationale:** Docker Compose is the standard tool for multi-container local development. Simple YAML configuration. Supported on all major platforms.

**Alternatives considered:**
- Kubernetes — rejected; excessive complexity for local development of an academic project.
- Manual process management — rejected; error-prone and non-reproducible.

**Consequences:** Each service needs a Dockerfile. Environment variables are managed via `.env` file.

---

### DEC-007: Migration Tool Selection

**Date:** 2026-10-07  
**Phase:** 0  
**Status:** ACCEPTED

**Context:** Database schema changes need to be tracked and applied reproducibly.

**Decision:** Use Flyway for database migrations. Flyway migrations V1–V7 have been implemented for Phase 2 (roles, users, documents, document_files, analysis_tasks, audit_logs, seed roles).

**Rationale:** Flyway is the standard migration tool for Spring Boot with excellent Maven/Gradle integration. SQL-based migrations are simple to read and maintain. Spring Boot auto-configures Flyway with zero additional setup. Hibernate DDL auto is set to `validate` in production to ensure the schema always matches the entities.

**Alternatives considered:**
- Liquibase — viable but XML/YAML changelogs add verbosity without clear benefit for this project.
- Hibernate DDL generation (dev only) — used in test profile (`create-drop` with H2), but not suitable for production where reproducible, version-controlled migrations are needed.

**Consequences:** All schema changes must go through Flyway migration files. The test profile uses H2 with Hibernate `create-drop` instead (Flyway disabled) for speed and isolation.

---

### DEC-008: JWT Signing Algorithm and Password Hashing

**Date:** 2026-10-20  
**Phase:** 2  
**Status:** ACCEPTED

**Context:** Phase 2 implements JWT authentication and user registration. Decisions were needed on the JWT signing algorithm, token expiry, and password hashing scheme.

**Decision:** Use HS256 for JWT signing with a configurable secret (minimum 32 bytes, auto-padded if shorter in dev). Token expiry is 24 hours (86400000 ms, configurable). Passwords are hashed with `BCryptPasswordEncoder` from Spring Security.

**Rationale:** HS256 is the default and simplest HMAC-based JWT algorithm — sufficient for a single-service backend where the secret stays server-side. BCrypt is the industry standard for password hashing with adaptive cost factor. Both are well-supported by Spring Security and JJWT 0.12.6.

**Alternatives considered:**
- RS256 (asymmetric) — unnecessary complexity for a single backend issuing and validating its own tokens.
- Argon2 — stronger than BCrypt but requires additional dependency; BCrypt is sufficient and widely supported.

**Consequences:** The JWT secret must be kept secure in production via environment variable. Token refresh is not implemented in Phase 2 (tokens expire after 24h).

---

### DEC-009: H2 In-Memory Database for Tests with PostgreSQL Compatibility Mode

**Date:** 2026-10-20  
**Phase:** 2  
**Status:** ACCEPTED

**Context:** Integration tests need a fast, self-contained database that doesn't require a running PostgreSQL instance. However, the production database is PostgreSQL and uses PostgreSQL-specific features (e.g., `gen_random_uuid()`, `JSONB`).

**Decision:** Use H2 in-memory database with `MODE=PostgreSQL` compatibility flag for tests. Flyway is disabled in tests; Hibernate `ddl-auto: create-drop` generates the schema from JPA entities. A `TestDataInitializer` seeds the three default roles in the test profile.

**Rationale:** H2 in PostgreSQL mode provides fast, isolated test execution without Docker. Hibernate-generated schema is sufficient for entity-level testing. Disabling Flyway in tests avoids migration syntax issues (e.g., `gen_random_uuid()` is PostgreSQL-specific). The `TestDataInitializer` compensates for the lack of Flyway V7 role seeding.

**Alternatives considered:**
- Testcontainers with real PostgreSQL — most accurate but slow (Docker startup overhead per test class). Can be added later for critical integration tests.
- Flyway with H2-compatible migrations — would require maintaining dual-syntax migrations, adding complexity.

**Consequences:** Tests run against H2, not real PostgreSQL. Subtle PostgreSQL-specific behavior differences (e.g., JSONB vs TEXT column types) are not caught by tests. Production uses Flyway + `ddl-auto: validate` to ensure schema correctness.

---

### DEC-010: Explainable Frequency-Domain and Statistical Baseline for AI Manipulation Screening

**Date:** 2026-10-25  
**Phase:** 9  
**Status:** ACCEPTED

**Context:** Phase 9 requires screening for possible AI-assisted manipulation (splicing, inpainting, generation) in identity documents. Deep learning detectors often require multi-gigabyte weight downloads, have high false-positive rates on compressed documents, and lack interpretability.

**Decision:** Implement an explainable baseline using four frequency-domain and statistical techniques:
1. **Spectral Analysis (2D FFT)**: detects high-frequency energy reduction and periodic GAN convolutional artifacts.
2. **JPEG Ghost Detection**: multi-quality re-compression (Q=50–90) identifies block error divergence indicating copy-paste splicing.
3. **Gradient Consistency Analysis**: Sobel gradient magnitude block variance detects unnaturally smooth inpainting regions.
4. **Texture Regularity Analysis**: Local Binary Pattern (LBP) histogram uniformity and entropy identify synthetic textures.

**Rationale:**
- Complies strictly with the master specification's mandate to "start with explainable baseline techniques" and never claim 100% certainty.
- Runs with minimal latency (~50ms) on CPU without heavy external model downloads or GPU requirements.
- Generates transparent, human-auditable spatial bounding boxes (suspicious regions) and confidence values.
- Serves as the benchmark baseline against which deep learning models can be evaluated in Phase 13.

**Consequences:**
- The module outputs risk scores and evidence signals, not legal verdicts.
- Highly compressed or resized images may attenuate high frequencies naturally; weighted scoring and fusion mitigates single-technique false positives.

---

### DEC-011: PDF Report Generation with Apache PDFBox

**Date:** 2026-11-03  
**Phase:** 12  
**Status:** ACCEPTED

**Context:** Phase 12 requires a downloadable screening report summarising all analysis results, risk assessment, module findings, and explanations in a portable, printable format.

**Decision:** Use Apache PDFBox 3.0.3 (Apache 2.0 license) to generate PDF reports server-side in `ReportService`. Reports are generated automatically after evidence fusion completes, stored on disk alongside uploaded documents, and served via authenticated REST endpoints. The frontend triggers download using a Blob-based fetch to `GET /api/documents/{id}/analysis/report/download`.

**Rationale:**
- PDFBox is a mature, pure-Java PDF library with no native dependencies — ideal for Spring Boot.
- Server-side generation ensures the report content matches exactly what was persisted in PostgreSQL (no client-side rendering discrepancies).
- PDF format is universally viewable, printable, and suitable for audit/archival purposes.
- Automatic generation after fusion ensures every completed analysis has a downloadable report without user intervention.

**Alternatives considered:**
- iText — more feature-rich but AGPL licensed, requiring a commercial license for proprietary use.
- Client-side PDF generation (jsPDF, html2pdf) — rejected; would require exposing all data to the browser and risk inconsistencies with server-side data.
- HTML report with browser print — rejected; not portable or archivable as a standalone file.

**Consequences:** Reports are stored as files on disk (alongside uploads). The `reports` table tracks metadata. Report file cleanup follows document deletion via `ON DELETE CASCADE`. The frontend API client gained blob download capability (`api.download()`) for binary responses.

---

### DEC-012: Synthetic Test Suite for Baseline Evaluation

**Date:** 2026-11-04  
**Phase:** 13  
**Status:** ACCEPTED

**Context:** Phase 13 requires evaluation of classification accuracy, localization quality, and robustness across degradation conditions. No real manipulated-document dataset with ground-truth labels was available.

**Decision:** Generate a synthetic test suite using Pillow/NumPy programmatic manipulations:
- 6 genuine certificate images (plain, with borders, gradient backgrounds, text-heavy, photo area, watermarked)
- 8 manipulated images covering text_edit, copy_paste, inpainting, generation, and resave categories
- 16 robustness-degraded variants (JPEG Q30/Q50, Gaussian blur r=2.0, resize 50%)
- 30 total test cases with exact ground-truth labels and bounding boxes
- Evaluate forensics and AI manipulation modules using Accuracy, Precision, Recall, F1, ROC-AUC, and IoU

**Rationale:** Synthetic data enables reproducible, fully automated evaluation without licensing external forensic datasets. Ground-truth labels and manipulation bounding boxes are exact (no annotation noise). The framework can be re-run with real data when available.

**Alternatives considered:**
- Real manipulated datasets (CASIA, Columbia, COVERAGE) — not used due to licensing restrictions and multi-GB download requirements.
- Manual human evaluation — not reproducible or scalable.
- Crowdsourced annotation — out of scope for an academic project.

**Consequences:**
- Forensics module (ELA/noise/edge) scores 0.0 F1 on synthetic data because programmatic PIL edits do not create the JPEG double-compression artifacts that ELA detects. This is a known limitation of the test methodology, not a module defect.
- AI manipulation module shows high false-positive rate on synthetic genuine images with large uniform certificate areas (spectral/gradient techniques trigger on uniform regions).
- Results are **indicative baselines**, not production accuracy claims.
- Framework is reusable: swap synthetic generator for real images to get production-grade metrics.

---

### DEC-013: Phase 14 Hardening Strategy

**Date:** 2026-11-16  
**Phase:** 14  
**Status:** ACCEPTED

**Context:** Phase 14 requires security hardening, input validation, error handling, performance optimization, and UX polish across all three services.

**Decision:** Implement the following hardening measures without introducing new dependencies or changing the existing architecture:
1. **Security headers** via servlet filter (CSP, HSTS, X-Frame-Options, X-Content-Type-Options, etc.)
2. **In-memory rate limiting** via servlet filter (per-IP, separate limits for auth and general endpoints)
3. **Magic bytes validation** in `StorageService` to prevent MIME type spoofing
4. **Expanded `GlobalExceptionHandler`** covering 10+ exception types with safe error messages (no stack trace leaks)
5. **Input sanitization** utility for pagination bounds, sort field whitelisting, and log injection prevention
6. **Response GZIP compression** for JSON/PDF responses > 1 KB
7. **RestTemplate timeouts** (10s connect, 120s read) on AI service client
8. **Python global exception handler** and request size guard
9. **React ErrorBoundary** and 404 page for frontend crash resilience
10. **Accessibility**: skip-to-content link, ARIA landmarks, focus-visible outlines, reduced-motion media query

**Rationale:** All hardening measures use Spring Boot / servlet built-ins and standard web security practices. No external rate-limiting infrastructure (Redis) is needed for a single-instance academic deployment. Magic bytes validation adds defence-in-depth beyond MIME type checking.

**Alternatives considered:**
- Spring Security rate limiting with Bucket4j — adds dependency complexity for a single-node deployment.
- Helmet.js for frontend headers — Spring Boot serves APIs only, not the React SPA; security headers are applied at API level.

**Consequences:** Rate limiting is in-memory (resets on restart, not distributed). For production, a Redis-backed or API gateway rate limiter would be needed. CSP policy is strict — if the frontend embeds external resources in the future, CSP must be updated.

---

*New decisions will be appended as they arise.*
