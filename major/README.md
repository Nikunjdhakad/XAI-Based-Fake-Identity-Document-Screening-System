# VeriLens

### Evidence-first document screening & risk assessment

VeriLens is an explainable, multi-signal AI-assisted document screening platform. It accepts identity documents and certificates, runs six independent analysis modules, fuses the evidence, and produces an auditable risk assessment with explanations and suspicious-region localization.

**VeriLens is a screening system.** It does not claim to legally prove that a document is genuine or fake. It uses careful language — "potential manipulation", "elevated risk", "evidence found" — and openly acknowledges its limitations.

---

## Table of Contents

- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Analysis Modules](#analysis-modules)
- [Repository Structure](#repository-structure)
- [Getting Started](#getting-started)
- [Running with Docker](#running-with-docker)
- [Local Development](#local-development)
- [API Reference](#api-reference)
- [Database Schema](#database-schema)
- [Testing](#testing)
- [Project Phases](#project-phases)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [Academic Context](#academic-context)
- [Disclaimer](#disclaimer)

---

## Architecture

```text
┌──────────────────────────────────────────────────────────────────┐
│                         Client Browser                           │
│                      React 19 + TypeScript                        │
└──────────────────────────────┬───────────────────────────────────┘
                               │ REST / JSON (JWT auth)
                               ▼
┌──────────────────────────────────────────────────────────────────┐
│                    Spring Boot Backend                            │
│                     Java 21 · Maven · Docker                      │
│                                                                   │
│   Auth (JWT)  ·  Documents (CRUD)  ·  Analysis (async jobs)       │
│   Evidence Fusion  ·  Risk Scoring  ·  Audit Trail                │
│                                                                   │
└──────────┬───────────────────────────────────┬────────────────────┘
           │                                   │ Internal REST
           │ JDBC/JPA                           │ (API key auth)
           ▼                                   ▼
┌──────────────────────┐         ┌─────────────────────────────────┐
│   PostgreSQL 16      │         │       Python AI Service          │
│                      │         │       FastAPI + Uvicorn          │
│  Users · Documents   │         │                                 │
│  Analysis Jobs        │         │  OCR (Tesseract)                │
│  Module Results       │         │  Image Forensics (OpenCV)       │
│  Risk Assessments     │         │  Metadata (Pillow + PyMuPDF)    │
│  Audit Logs           │         │  Identity Consistency (rules)   │
│                      │         │  Face Verification (Haar + HOG)  │
│                      │         │  AI Manipulation (FFT + stats)   │
└──────────────────────┘         └─────────────────────────────────┘
```

### Communication Rules

| Boundary | Protocol | Auth |
|---|---|---|
| React → Spring Boot | REST / JSON | JWT Bearer token |
| Spring Boot → Python AI Service | Internal REST / multipart | API key header |
| Spring Boot → PostgreSQL | JDBC via Spring Data JPA | Database credentials |
| Python → PostgreSQL | **Never** | Python does not access the database |

All services run in Docker containers orchestrated by Docker Compose.

---

## Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| **Frontend** | React 19, TypeScript, Vite, CSS Modules | UI, navigation, result visualisation |
| **Backend** | Java 21, Spring Boot 3.3.4, Spring Security, JPA, Flyway, Maven | Auth, business logic, orchestration, persistence |
| **AI Service** | Python 3.11, FastAPI, OpenCV, Tesseract, Pillow, PyMuPDF, NumPy | OCR, forensics, metadata, face, AI manipulation |
| **Database** | PostgreSQL 16 | Users, documents, analysis jobs, results, audit |
| **Infrastructure** | Docker, Docker Compose | Container orchestration for local dev |

### Key Libraries

| Service | Library | Purpose |
|---|---|---|
| Backend | `jjwt` 0.12.6 | JWT generation and validation |
| Backend | `flyway-core` | Database schema migrations (V1–V16) |
| Backend | `spring-boot-starter-security` | Authentication, authorization, CORS |
| AI Service | `pytesseract` | OCR text extraction |
| AI Service | `opencv-python-headless` | Image forensics, face detection |
| AI Service | `PyMuPDF` (fitz) | PDF metadata extraction |
| AI Service | `pdf2image` | PDF to image conversion |
| Frontend | `react-router-dom` | Client-side routing |

---

## Analysis Modules

Each module produces **evidence**, not a verdict. The Evidence Fusion layer combines all module outputs into a single risk assessment.

| # | Module | Technology | What it detects |
|---|---|---|---|
| 1 | **OCR** | Tesseract via pytesseract | Extracted text, word bounding boxes, confidence scores |
| 2 | **Image Forensics** | OpenCV (ELA, noise, edges) | Compression error regions, noise inconsistency, edge density anomalies |
| 3 | **Metadata Analysis** | Pillow, PyMuPDF | EXIF data, PDF metadata, editing software signatures, timestamp anomalies |
| 4 | **Identity Consistency** | Python regex + rules | Date format validation, date chronology, field presence, name consistency |
| 5 | **Face Verification** | OpenCV Haar Cascade + HOG features | Face detection, cosine similarity (POSSIBLE_MATCH / POSSIBLE_MISMATCH / NOT_APPLICABLE) |
| 6 | **AI Manipulation** | OpenCV FFT + statistics | Spectral anomalies, JPEG ghost artifacts, gradient smoothness, texture regularity |

### Evidence Fusion (Phase 10)

Weighted baseline v1.0.0 — transparent and auditable:

| Module | Weight | Description |
|---|---|---|
| Image Forensics | 25% | Primary visual manipulation indicators |
| AI Manipulation | 25% | AI-generated/inpainted content screening |
| Metadata | 15% | File-level anomaly evidence |
| Identity | 15% | Internal consistency checks |
| OCR | 10% | Text extraction quality (failed OCR = minor risk) |
| Face | 10% | Face match/mismatch signal (only when reference provided) |

**Risk levels:** LOW (0.00–0.33) · MEDIUM (0.34–0.66) · HIGH (0.67–1.00)

These thresholds are **initial estimates** and will be refined during evaluation.

---

## Repository Structure

```text
major/
├── frontend/                 React 19 + TypeScript + Vite
│   ├── src/
│   │   ├── api/             API client (fetch + JWT)
│   │   ├── components/      Shared UI (AppLayout, RiskBadge, StatusBadge, ProtectedRoute)
│   │   ├── context/         AuthContext (JWT management)
│   │   ├── pages/           7 pages (Login, Dashboard, Upload, Analysis, Results, History, Details)
│   │   ├── types.ts         TypeScript interfaces for all API types
│   │   └── utils/           JSON parsing utilities
│   ├── package.json
│   ├── vite.config.ts
│   └── Dockerfile
│
├── backend/                  Spring Boot 3.3.4 + Java 21
│   ├── src/main/java/com/verilens/
│   │   ├── config/          Security, Async, AI service, Exception handler, Test data
│   │   ├── controller/      Auth, Document, Analysis, Health controllers
│   │   ├── dto/             Request/response DTOs
│   │   ├── entity/          JPA entities (User, Document, AnalysisTask, OcrResult, etc.)
│   │   ├── repository/      Spring Data JPA repositories
│   │   ├── security/        JWT provider, filter, UserDetailsService
│   │   └── service/         Auth, Document, Analysis, EvidenceFusion, AiServiceClient, Storage, Audit
│   ├── src/main/resources/
│   │   ├── application.yml   Production config
│   │   └── db/migration/    Flyway SQL migrations (V1–V16)
│   ├── src/test/            Integration tests (24 tests)
│   ├── pom.xml              Maven configuration
│   └── Dockerfile
│
├── ai-service/              Python 3.11 + FastAPI
│   ├── app/
│   │   ├── main.py          FastAPI app, health + analyze endpoints
│   │   └── modules/          OCR, forensics, metadata, identity, face, ai_manipulation
│   ├── tests/               Pytest test suites (136 tests)
│   ├── requirements.txt      Python dependencies
│   └── Dockerfile           Includes Tesseract + poppler
│
├── database/
│   └── init/                PostgreSQL init script
│
├── docs/                    Full project documentation
│   ├── architecture.md      Service boundaries, data flow
│   ├── api-contract.md      REST endpoint specifications
│   ├── database-design.md  Entity definitions, ER diagram
│   ├── ai-modules.md        Module specs, input/output schemas
│   ├── ui-ux-spec.md        Design direction, screen layouts
│   ├── testing-strategy.md  Test plan per service and phase
│   ├── decisions.md         Architectural decision log (DEC-001 to DEC-009)
│   └── implementation-status.md  Phase tracking
│
├── docker-compose.yml       Service orchestration
├── .env.example             Environment variable template
├── .gitignore
└── README.md
```

---

## Getting Started

### Prerequisites

- **Docker Desktop** (with Docker Compose)
- **Java 21 LTS** (for local backend development)
- **Node.js 18+** (for local frontend development)
- **Python 3.11+** (for local AI service development)
- **Maven 3.9+** (for backend builds)

### Quick Start (Docker)

```bash
# 1. Clone the repository
git clone <repository-url>
cd major

# 2. Create environment file
cp .env.example .env
# Edit .env and set passwords/keys

# 3. Build and start all services
docker compose up --build -d

# 4. Verify services are running
docker compose ps
```

| Service | URL | Port |
|---|---|---|
| Frontend | http://localhost:3000 | 3000 |
| Backend API | http://localhost:8080 | 8080 |
| AI Service | http://localhost:8000 | 8000 |
| PostgreSQL | localhost | 5432 |

### First-time Usage

1. Open http://localhost:3000
2. Create an account (Sign In → Create Account tab)
3. Upload a document (New Screening)
4. Start analysis
5. View results with module breakdown and risk assessment

---

## Running with Docker

### Start all services

```bash
docker compose up -d
```

### Rebuild after code changes

```bash
# Rebuild a specific service
docker compose build backend
docker compose up -d backend

# Rebuild all services
docker compose up --build -d
```

### View logs

```bash
docker compose logs -f backend
docker compose logs -f ai-service
docker compose logs -f frontend
```

### Stop all services

```bash
docker compose down

# Also remove volumes (destroys database data)
docker compose down -v
```

### Health checks

```bash
# Backend health
curl http://localhost:8080/api/health

# AI service health
curl http://localhost:8000/internal/health
```

---

## Local Development

### Backend (Spring Boot)

```bash
cd backend
# Run tests
mvn test
# Build
mvn package -DskipTests
# Run locally (requires PostgreSQL running)
mvn spring-boot:run
```

### Frontend (React)

```bash
cd frontend
npm install
npm run dev      # Dev server with hot reload (port 5173)
npm run build    # Production build
npm run preview  # Preview production build
```

The Vite dev server proxies `/api` to `http://localhost:8080` for local development.

### AI Service (Python)

```bash
cd ai-service
pip install -r requirements.txt
# Requires Tesseract OCR installed: apt install tesseract-ocr (Linux)
pytest                          # Run tests
uvicorn app.main:app --reload   # Dev server (port 8000)
```

---

## API Reference

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Create account |
| POST | `/api/auth/login` | Login, receive JWT |
| GET | `/api/auth/me` | Get current user profile |

### Documents

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/documents` | Upload document (multipart) |
| GET | `/api/documents` | List documents (paginated) |
| GET | `/api/documents/{id}` | Get document details |
| DELETE | `/api/documents/{id}` | Delete document |

### Analysis

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/documents/{id}/analysis` | Start analysis (async) |
| GET | `/api/documents/{id}/analysis` | Get analysis task |
| GET | `/api/documents/{id}/analysis/status` | Poll analysis status |
| GET | `/api/documents/{id}/analysis/ocr` | OCR results |
| GET | `/api/documents/{id}/analysis/metadata` | Metadata results |
| GET | `/api/documents/{id}/analysis/identity` | Identity consistency results |
| GET | `/api/documents/{id}/analysis/forensics` | Forensics results |
| GET | `/api/documents/{id}/analysis/face` | Face verification results |
| GET | `/api/documents/{id}/analysis/ai-manipulation` | AI manipulation results |
| GET | `/api/documents/{id}/analysis/risk` | Risk assessment (evidence fusion) |

### Internal (AI Service)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/internal/health` | None | Health + module availability |
| POST | `/internal/analyze` | API key | Run analysis modules |

Full request/response schemas: see [docs/api-contract.md](docs/api-contract.md)

---

## Database Schema

16 Flyway migrations create the following tables:

| Migration | Table | Purpose |
|---|---|---|
| V1 | `roles` | USER, ANALYST, ADMIN |
| V2 | `users` | User accounts (bcrypt password hashing) |
| V3 | `documents` | Uploaded document metadata |
| V4 | `document_files` | Physical file tracking (document + reference face) |
| V5 | `analysis_tasks` | Analysis job lifecycle |
| V6 | `audit_logs` | Security event log |
| V7 | (seed) | Default role insertion |
| V8 | `ocr_results` | OCR module output |
| V9 | `metadata_results` | Metadata module output |
| V10 | `identity_consistency_results` | Identity module output |
| V11 | `forensic_results` | Forensics module output |
| V12 | `face_verification_results` | Face verification output |
| V13 | `ai_manipulation_results` | AI manipulation output |
| V15 | `risk_assessments` | Evidence fusion risk assessment |
| V16 | (alter) | Add `risk_level` column to documents |

Full schema: see [docs/database-design.md](docs/database-design.md)

---

## Testing

| Service | Tool | Command | Tests |
|---|---|---|---|
| Backend | JUnit 5 + Spring Boot Test | `mvn test` | 24 integration tests |
| AI Service | pytest | `pytest tests/` | 136 tests (1 skipped) |
| Frontend | TypeScript build | `npm run build` | Type checking + Vite build |

### Test Coverage

**Backend tests:**
- `AuthControllerTest` (12) — registration, login, JWT, validation, duplicates
- `DocumentControllerTest` (9) — upload, retrieval, deletion, access control
- `HealthControllerTest` (2) — health endpoint, public access
- `VeriLensApplicationTests` (1) — Spring context loads

**AI Service tests:**
- `test_health.py` (2) — health endpoint, API key
- `test_ocr.py` (17) — OCR module schema, endpoint, edge cases
- `test_metadata.py` (20) — metadata schema, anomalies, evidence score
- `test_identity.py` (25) — consistency rules, date detection, endpoint
- `test_forensics.py` (17) — ELA, noise, edge, findings, regions
- `test_face.py` (15) — NOT_APPLICABLE, similarity, endpoint
- `test_ai_manipulation.py` (40) — FFT, JPEG ghost, gradient, texture, risk score

---

## Project Phases

| Phase | Description | Status |
|---|---|---|
| 0 | Specification | ✅ Complete |
| 1 | Repository Foundation | ✅ Complete |
| 2 | Backend Foundation | ✅ Complete |
| 3 | Frontend Foundation | ✅ Complete |
| 4 | OCR | ✅ Complete |
| 5 | Metadata Analysis | ✅ Complete |
| 6 | Identity Consistency | ✅ Complete |
| 7 | Image Forensics | ✅ Complete |
| 8 | Face Verification | ✅ Complete |
| 9 | AI Manipulation Screening | ✅ Complete |
| 10 | Evidence Fusion & Risk Scoring | ✅ Complete |
| 11 | Suspicious Region Viewer | ✅ Complete |
| 12 | Reports | ⬜ Not Started |
| 13 | Evaluation | ⬜ Not Started |
| 14 | Hardening | ⬜ Not Started |
| 15 | Final Documentation | ⬜ Not Started |

Detailed status: see [docs/implementation-status.md](docs/implementation-status.md)

---

## Documentation

| Document | Contents |
|---|---|
| [architecture.md](docs/architecture.md) | Service boundaries, data flow, deployment topology |
| [api-contract.md](docs/api-contract.md) | REST endpoint specifications with examples |
| [database-design.md](docs/database-design.md) | Entity definitions, ER diagram, indexes |
| [ai-modules.md](docs/ai-modules.md) | Module specs, input/output schemas, status |
| [ui-ux-spec.md](docs/ui-ux-spec.md) | Design direction, screen layouts, accessibility |
| [testing-strategy.md](docs/testing-strategy.md) | Test plan per service and phase |
| [decisions.md](docs/decisions.md) | Architectural decision log (9 decisions) |
| [implementation-status.md](docs/implementation-status.md) | Phase tracking with details |

---

## Contributing

### Code Style

- **Backend:** Standard Java conventions, Spring Boot best practices, JPA entities with explicit getters/setters
- **AI Service:** Python PEP 8, typed function signatures, docstrings on public functions
- **Frontend:** TypeScript strict mode, CSS Modules, functional components with hooks
- **Database:** Flyway versioned migrations only (never modify applied migrations)

### Branch Strategy

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Make changes, ensure tests pass
3. Commit with clear messages
4. Push and create a pull request

### Commit Message Convention

```
<type>: <description>

[optional body]

Types: feat, fix, docs, refactor, test, chore, phase
```

### Rules (from specification)

- Work on **one phase at a time**
- Do not silently change the technology stack
- Do not introduce duplicate frameworks
- Do not rewrite working modules without reason
- Run tests after every phase
- Update documentation after every phase
- Never create fake/mock AI results in production code
- Keep commits grouped by phase

---

## Academic Context

This project is a major academic project demonstrating:

**Engineering contribution:** Full-stack application with Spring Boot backend, React frontend, PostgreSQL persistence, microservice architecture, JWT authentication, async job processing, file handling, evidence fusion, and audit trail.

**Research/AI contribution:** Multi-signal document analysis combining OCR, image forensics, metadata analysis, identity consistency checking, face verification, and AI manipulation screening, with evidence fusion, explainable risk assessment, and structured evidence output.

The project is presented as an **explainable, multi-signal AI-assisted document screening and risk-assessment platform** — not a perfect fake-document detector.

---

## Disclaimer

VeriLens is a **screening and risk-assessment system**. It is:

- **Not** a legal authenticity authority
- **Not** a government verification service
- **Not** a guaranteed fake-document detector

The system openly acknowledges:
- No 100% detection guarantee
- AI detectors can fail
- Metadata can be missing or spoofed
- OCR can contain errors
- Face verification is a similarity signal, not legal identity proof
- Unseen forgery techniques remain challenging
- Degraded images reduce performance
- Legal authenticity is outside project scope

These limitations make the project more credible, not weaker.
