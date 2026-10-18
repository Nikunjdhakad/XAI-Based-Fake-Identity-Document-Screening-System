# VeriLens — Decision Log

**Last updated:** 2026-10-07

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
**Status:** PENDING

**Context:** Database schema changes need to be tracked and applied reproducibly.

**Decision:** To be decided in Phase 1 or Phase 2. Options: Flyway, Liquibase, or Hibernate DDL generation (dev only).

**Rationale:** Deferring because development-mode Hibernate DDL is sufficient for Phase 1. A proper migration tool will be selected when schema stabilizes.

**Consequences:** Initial development uses Hibernate DDL auto. A migration tool will be introduced before production-like deployments.

---

*New decisions will be appended as they arise.*
