# VeriLens

**Evidence-first document screening.**

---

## What is VeriLens?

VeriLens is an explainable, multi-signal AI-assisted document screening and risk-assessment platform. It accepts identity documents and certificates, performs analysis across six evidence-producing modules, fuses the results, and produces an auditable screening report with a risk assessment.

VeriLens is a **screening system** — it does not claim to legally prove that a document is genuine or fake. It produces risk assessments with supporting evidence and explanations.

---

## Architecture

```text
┌─────────────────┐
│  React Frontend │
│  (TypeScript)   │
└────────┬────────┘
         │ REST / JSON
         ▼
┌─────────────────┐       ┌─────────────────────┐
│  Spring Boot    │──────▶│  Python AI Service   │
│  Backend        │       │  (FastAPI)           │
│  (Java 21)      │       │                     │
└────────┬────────┘       │  OCR · Forensics    │
         │                │  Metadata · Face     │
         ▼                │  AI Manipulation     │
┌─────────────────┐       └─────────────────────┘
│  PostgreSQL 16  │
└─────────────────┘
```

### Communication Rules

- React communicates **only** with Spring Boot
- Spring Boot orchestrates all business logic and calls Python internally
- Python returns structured JSON evidence — it never accesses PostgreSQL directly
- All services are containerized with Docker

---

## Technology Stack

| Layer      | Technology                                              |
| ---------- | ------------------------------------------------------- |
| Frontend   | React 18+, TypeScript, Vite, CSS Modules                |
| Backend    | Java 21, Spring Boot 3.x, Spring Security, JWT, Maven   |
| Database   | PostgreSQL 16                                           |
| AI Service | Python 3.11+, FastAPI, OpenCV, PaddleOCR/Tesseract      |
| Infra      | Docker, Docker Compose                                  |

---

## Repository Structure

```text
fake-document-screening/
│
├── frontend/              React application
├── backend/               Spring Boot application
├── ai-service/            Python FastAPI AI service
├── database/              Schema and migrations
├── docs/                  Project documentation
│   ├── architecture.md
│   ├── api-contract.md
│   ├── database-design.md
│   ├── ai-modules.md
│   ├── ui-ux-spec.md
│   ├── testing-strategy.md
│   ├── decisions.md
│   └── implementation-status.md
│
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

---

## Analysis Modules

VeriLens performs document analysis through six evidence-producing modules:

| Module                    | Purpose                                        |
| ------------------------- | ---------------------------------------------- |
| **OCR**                   | Extract text and spatial information            |
| **Image Forensics**       | Detect image-level manipulation indicators      |
| **Metadata Analysis**     | Analyze file metadata for inconsistencies       |
| **Identity Consistency**  | Check internal field consistency                 |
| **Face Verification**     | Compare document face against reference (optional) |
| **AI Manipulation Screening** | Screen for AI-assisted manipulation         |

Module outputs are combined by the **Evidence Fusion** layer, which produces a final **Risk Assessment** (LOW / MEDIUM / HIGH) with explanations and suspicious-region localization.

No individual module claims to prove a document is fake.

---

## Development Phases

| Phase | Description                    | Status         |
| ----- | ------------------------------ | -------------- |
| 0     | Specification                  | ✅ Complete     |
| 1     | Repository Foundation          | ⬜ Not Started |
| 2     | Backend Foundation             | ⬜ Not Started |
| 3     | Frontend Foundation            | ⬜ Not Started |
| 4     | OCR                            | ⬜ Not Started |
| 5     | Metadata Analysis              | ⬜ Not Started |
| 6     | Identity Consistency           | ⬜ Not Started |
| 7     | Image Forensics                | ⬜ Not Started |
| 8     | Face Verification              | ⬜ Not Started |
| 9     | AI Manipulation Screening      | ⬜ Not Started |
| 10    | Evidence Fusion & Risk Scoring | ⬜ Not Started |
| 11    | Suspicious Region Viewer       | ⬜ Not Started |
| 12    | Reports                        | ⬜ Not Started |
| 13    | Evaluation                     | ⬜ Not Started |
| 14    | Hardening                      | ⬜ Not Started |
| 15    | Final Documentation            | ⬜ Not Started |

See [docs/implementation-status.md](docs/implementation-status.md) for detailed status tracking.

---

## Getting Started

> **Note:** Services are not yet runnable. Phase 1 will create the application code, Dockerfiles, and health endpoints.

### Prerequisites

- Docker & Docker Compose
- Java 21 (for backend development)
- Node.js 18+ (for frontend development)
- Python 3.11+ (for AI service development)

### Setup

1. Clone the repository
2. Copy `.env.example` to `.env` and fill in values
3. Run `docker-compose up` (available after Phase 1)

---

## Documentation

| Document                                              | Contents                              |
| ----------------------------------------------------- | ------------------------------------- |
| [Architecture](docs/architecture.md)                  | Service boundaries, data flow         |
| [API Contract](docs/api-contract.md)                  | REST endpoint specifications          |
| [Database Design](docs/database-design.md)            | Entity definitions, relationships     |
| [AI Modules](docs/ai-modules.md)                      | Module specs, input/output schemas    |
| [UI/UX Spec](docs/ui-ux-spec.md)                      | Design direction, screen layouts      |
| [Testing Strategy](docs/testing-strategy.md)          | Test plan per service and phase       |
| [Decisions](docs/decisions.md)                         | Architectural decision log            |
| [Implementation Status](docs/implementation-status.md)| Phase tracking                        |

---

## Academic Context

This project is a major academic project demonstrating:

**Engineering contribution:** Full-stack application with Spring Boot backend, React frontend, PostgreSQL persistence, service architecture, authentication, file processing, APIs, report generation, and audit trail.

**Research/AI contribution:** Multi-signal document analysis combining OCR, image forensics, metadata analysis, identity consistency checking, face verification, and AI manipulation screening, with evidence fusion, explainable risk assessment, and robustness evaluation.

### Important Disclaimer

VeriLens is a screening and risk-assessment system. It is **not** a legal authenticity authority, government verification service, or guaranteed fake-document detector. It openly acknowledges that AI detection can fail, OCR contains errors, metadata can be missing or spoofed, and unseen forgery techniques remain challenging.

---

## License

To be determined.
