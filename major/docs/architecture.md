# VeriLens — Architecture Specification

**Version:** 0.1.0  
**Phase:** 2 — Backend Foundation  
**Last updated:** 2026-10-20

---

## 1. System Overview

VeriLens is an explainable, multi-signal AI-assisted document screening and risk-assessment platform. It accepts identity documents and certificates, performs analysis across six evidence-producing modules, fuses the results, and produces an auditable risk assessment.

VeriLens is a **screening system** — it does not claim legal authenticity or replace institutional verification.

---

## 2. Service Architecture

```text
┌──────────────────────────────────────────────────────────────┐
│                       Client Browser                         │
│                     React + TypeScript                        │
└──────────────────────┬───────────────────────────────────────┘
                       │ REST / JSON
                       ▼
┌──────────────────────────────────────────────────────────────┐
│                  Spring Boot Backend                         │
│                                                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────────┐   │
│  │   Auth   │ │Documents │ │ Analysis │ │   Reports     │   │
│  │ (JWT)    │ │ (CRUD)   │ │ (Jobs)   │ │ (Generation)  │   │
│  └──────────┘ └──────────┘ └────┬─────┘ └───────────────┘   │
│                                  │                            │
│  ┌──────────────────────────────┐│ ┌─────────────────────┐   │
│  │   Evidence Fusion            ││ │   Audit Trail       │   │
│  │   Risk Score Engine          ││ │                     │   │
│  └──────────────────────────────┘│ └─────────────────────┘   │
└──────────────────┬───────────────┼───────────────────────────┘
                   │               │ Internal REST
                   ▼               ▼
┌─────────────────────┐   ┌───────────────────────────────────┐
│    PostgreSQL 16     │   │      Python AI Service            │
│                      │   │      FastAPI + Uvicorn            │
│  Users, Documents,   │   │                                   │
│  Jobs, Results,      │   │  ┌─────┐ ┌──────────┐ ┌───────┐  │
│  Evidence, Reports,  │   │  │ OCR │ │Forensics │ │ Meta  │  │
│  Audit               │   │  └─────┘ └──────────┘ └───────┘  │
│                      │   │  ┌─────┐ ┌──────────┐ ┌───────┐  │
│                      │   │  │Face │ │AI Manip. │ │Preproc│  │
│                      │   │  └─────┘ └──────────┘ └───────┘  │
└─────────────────────┘   └───────────────────────────────────┘
```

---

## 3. Service Boundaries

### 3.1 React Frontend

| Aspect       | Detail                                    |
| ------------ | ----------------------------------------- |
| **Role**     | User interface and presentation           |
| **Stack**    | React 18+, TypeScript, Vite, CSS Modules  |
| **Calls**    | Spring Boot REST API only                 |
| **Must not** | Call Python directly; contain ML logic    |

### 3.2 Spring Boot Backend

| Aspect       | Detail                                           |
| ------------ | ------------------------------------------------ |
| **Role**     | Central orchestration, business logic, data layer|
| **Stack**    | Java 21, Spring Boot 3.x, Maven                 |
| **Owns**     | Auth, users, documents, jobs, evidence, risk scores, reports, audit |
| **Calls**    | PostgreSQL (JPA), Python AI Service (REST)       |

### 3.3 Python AI Service

| Aspect       | Detail                                    |
| ------------ | ----------------------------------------- |
| **Role**     | AI/CV inference only                      |
| **Stack**    | Python 3.11+, FastAPI, Uvicorn            |
| **Owns**     | OCR, forensics, metadata extraction, face analysis, AI manipulation detection, preprocessing |
| **Returns**  | Structured JSON evidence                  |
| **Must not** | Write to PostgreSQL; be exposed to frontend|

### 3.4 PostgreSQL

| Aspect       | Detail                                    |
| ------------ | ----------------------------------------- |
| **Role**     | Persistent structured storage             |
| **Version**  | 16                                        |
| **Accessed by** | Spring Boot only                       |

---

## 4. Communication Flow

```text
React  ──REST/JSON──▶  Spring Boot  ──REST/JSON──▶  Python AI Service
                            │
                            ▼
                       PostgreSQL
```

- Frontend → Backend: Public REST API with JWT authentication
- Backend → AI Service: Internal REST with API key authentication
- Backend → PostgreSQL: JDBC via Spring Data JPA

---

## 5. Data Flow — Document Analysis

```text
1. User uploads document via React
2. React calls POST /api/documents (Spring Boot)
3. Spring Boot validates, stores file, creates Document + AnalysisJob records
4. Spring Boot calls POST /internal/analyze (Python)
5. Python runs analysis modules, returns JSON evidence
6. Spring Boot persists results to PostgreSQL
7. Spring Boot runs evidence fusion and risk scoring
8. Spring Boot returns analysis status to React
9. React renders results
```

---

## 6. Analysis Pipeline

```text
Document
   │
   ├──▶ OCR ──────────────────┐
   ├──▶ Image Forensics ──────┤
   ├──▶ Metadata Analysis ────┤
   ├──▶ Identity Consistency ─┼──▶ Evidence Fusion ──▶ Risk Engine ──▶ Report
   ├──▶ Face Verification ────┤
   └──▶ AI Manipulation ──────┘
```

Each module produces **evidence**, not verdicts. The fusion layer combines evidence into a risk assessment.

---

## 7. Security Architecture

| Concern                  | Mechanism                          |
| ------------------------ | ---------------------------------- |
| Authentication           | JWT tokens via Spring Security     |
| Password storage         | bcrypt hashing                     |
| Authorization            | Role-based (ADMIN, ANALYST, USER)  |
| Frontend ↔ Backend       | JWT in Authorization header        |
| Backend ↔ AI Service     | Internal API key                   |
| File uploads             | Size limit, MIME validation, safe naming |
| AI Service exposure      | Not accessible from frontend       |
| Audit                    | All significant actions logged     |
| CORS                     | Configured for frontend origin only|

---

## 8. Deployment Topology

All services are containerized with Docker and orchestrated by Docker Compose for local development.

```text
┌─────────────────────────────────────────────┐
│              Docker Compose                  │
│                                              │
│  ┌─────────┐  ┌─────────┐  ┌────────────┐   │
│  │Frontend │  │Backend  │  │AI Service  │   │
│  │ :3000   │  │ :8080   │  │ :8000      │   │
│  └─────────┘  └────┬────┘  └────────────┘   │
│                    │                          │
│               ┌────┴────┐                    │
│               │Postgres │                    │
│               │ :5432   │                    │
│               └─────────┘                    │
└─────────────────────────────────────────────┘
```

---

## 9. Key Architectural Decisions

See [decisions.md](decisions.md) for the full decision log.

| Decision                      | Rationale                                          |
| ----------------------------- | -------------------------------------------------- |
| Monorepo                      | Simplifies development for a single-team project   |
| Spring Boot as orchestrator   | Primary Java demonstration; central business logic  |
| Separate Python service       | Java is not practical for OCR/CV/ML inference       |
| Internal REST (not gRPC)      | Simpler; sufficient for this project's scale        |
| PostgreSQL (not NoSQL)        | Structured relational data fits the domain          |
| JWT (not sessions)            | Stateless auth; aligns with REST API architecture   |
| Docker Compose                | Simple local orchestration; no Kubernetes needed    |

---

## 10. Non-Functional Requirements

| Concern        | Target                                                  |
| -------------- | ------------------------------------------------------- |
| Scalability    | Single-instance sufficient for academic demonstration   |
| Availability   | Local development; no HA requirement                    |
| Performance    | Analysis within 30–60 seconds per document (target)     |
| Security       | JWT, input validation, safe file handling, audit trail  |
| Observability  | Structured logging per service; audit log in database   |
| Testability    | Unit + integration tests per service; ML evaluation     |
