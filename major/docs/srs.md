# VeriLens — Software Requirements Specification (SRS)

**Version:** 1.0.0  
**Date:** 2026-11-16  
**Project:** AI-Based Fake Identity & Document Screening System

---

## 1. Introduction

### 1.1 Purpose

This document specifies the software requirements for **VeriLens**, an AI-assisted document screening and risk-assessment platform. VeriLens accepts identity documents and certificates, performs multi-signal analysis for potential manipulation, and produces an explainable risk assessment.

### 1.2 Scope

VeriLens is a screening system — it does **not** claim legal authenticity, does not connect to government databases, and does not replace institutional verification. The system produces risk assessments (LOW / MEDIUM / HIGH) with supporting evidence and explanations.

### 1.3 Definitions

| Term | Definition |
|------|-----------|
| Screening | Automated inspection to identify potential anomalies |
| Risk Assessment | Composite score from multiple evidence sources |
| Evidence | Structured output from an analysis module |
| Fusion | Combining module evidence into a unified risk score |
| Module | An independent analysis component (OCR, forensics, etc.) |

---

## 2. Overall Description

### 2.1 Product Perspective

VeriLens is a standalone web application with three services:

1. **React Frontend** — user interface
2. **Spring Boot Backend** — orchestration, business logic, persistence
3. **Python AI Service** — computer vision and AI analysis

### 2.2 Product Functions

1. User registration and authentication
2. Document upload with file validation
3. Optional reference face image upload
4. Document type selection
5. Automated multi-module analysis
6. Real-time analysis progress tracking
7. Explainable risk scoring with evidence
8. Suspicious region visualization
9. PDF screening report generation
10. Screening history with search/filter
11. Audit trail for all operations

### 2.3 User Classes

| Role | Capabilities |
|------|-------------|
| USER | Upload documents, view own analyses, download reports |
| ANALYST | All USER capabilities + extended filtering |
| ADMIN | All capabilities + user management (future) |

### 2.4 Operating Environment

- Docker-containerized deployment
- PostgreSQL 16 database
- Runs on any system with Docker and Docker Compose
- Browser: Modern browsers (Chrome, Firefox, Edge, Safari)

---

## 3. Functional Requirements

### 3.1 Authentication (FR-01 to FR-03)

| ID | Requirement |
|----|------------|
| FR-01 | System shall allow user registration with username, email, password, and full name |
| FR-02 | System shall authenticate users via JWT tokens (24-hour expiry) |
| FR-03 | System shall enforce minimum password length of 8 characters |

### 3.2 Document Management (FR-04 to FR-08)

| ID | Requirement |
|----|------------|
| FR-04 | System shall accept PDF, JPEG, and PNG document uploads up to 20 MB |
| FR-05 | System shall validate file extension, MIME type, and magic bytes |
| FR-06 | System shall generate UUID-based safe filenames (no path traversal) |
| FR-07 | System shall optionally accept a reference face image for face verification |
| FR-08 | System shall allow document deletion with CASCADE to all related data |

### 3.3 Analysis Pipeline (FR-09 to FR-16)

| ID | Requirement |
|----|------------|
| FR-09 | System shall perform OCR using Tesseract with per-word confidence scores |
| FR-10 | System shall analyze file metadata for anomalies (editing software, timestamps, GPS) |
| FR-11 | System shall check extracted fields for identity consistency (dates, names, IDs) |
| FR-12 | System shall perform image forensics (ELA, noise analysis, edge density) |
| FR-13 | System shall perform face verification when a reference face is provided |
| FR-14 | System shall screen for AI manipulation (spectral, JPEG ghost, gradient, texture) |
| FR-15 | System shall fuse all module evidence into a weighted risk score (0.0–1.0) |
| FR-16 | System shall classify risk as LOW (0.00–0.33), MEDIUM (0.34–0.66), HIGH (0.67–1.00) |

### 3.4 Results & Reporting (FR-17 to FR-21)

| ID | Requirement |
|----|------------|
| FR-17 | System shall display analysis results with per-module detail accordions |
| FR-18 | System shall visualize suspicious regions as bounding box overlays |
| FR-19 | System shall generate a PDF screening report automatically after analysis |
| FR-20 | System shall allow authenticated download of screening reports |
| FR-21 | System shall maintain screening history with pagination, search, and status filter |

### 3.5 Audit & Security (FR-22 to FR-25)

| ID | Requirement |
|----|------------|
| FR-22 | System shall log all significant actions (register, login, upload, delete, analysis) |
| FR-23 | System shall rate-limit API requests (60/min general, 10/min auth) |
| FR-24 | System shall include security headers on all responses (CSP, HSTS, X-Frame-Options) |
| FR-25 | System shall never expose stack traces or internal errors to clients |

---

## 4. Non-Functional Requirements

### 4.1 Performance

| ID | Requirement |
|----|------------|
| NFR-01 | Single document analysis shall complete within 60 seconds |
| NFR-02 | API responses shall use GZIP compression for payloads > 1 KB |
| NFR-03 | Frontend bundle shall be < 350 KB (gzipped < 100 KB) |

### 4.2 Security

| ID | Requirement |
|----|------------|
| NFR-04 | Passwords shall be stored using bcrypt hashing |
| NFR-05 | JWT secrets shall be externalized via environment variables |
| NFR-06 | AI service shall not be accessible from the frontend |
| NFR-07 | File uploads shall be validated at extension, MIME, and content level |

### 4.3 Usability

| ID | Requirement |
|----|------------|
| NFR-08 | UI shall follow dark forensics-workstation aesthetic |
| NFR-09 | UI shall support keyboard navigation with visible focus indicators |
| NFR-10 | UI shall respect `prefers-reduced-motion` media query |
| NFR-11 | Language shall use "potential manipulation", never "definitely fake" |

### 4.4 Reliability

| ID | Requirement |
|----|------------|
| NFR-12 | Individual module failures shall not crash the entire analysis pipeline |
| NFR-13 | React ErrorBoundary shall catch rendering errors with fallback UI |

---

## 5. Supported Document Categories

- Identity documents (ID cards, passports, driver's licenses)
- Educational certificates (degrees, diplomas, transcripts)
- Experience / training certificates
- Institutional documents
- Selected reports and forms

---

## 6. Target Manipulation Types

- Text modification
- Copy-paste manipulation
- Image replacement
- AI-based inpainting
- Digital editing
- Re-saved or re-captured edited files

---

## 7. Explicitly Out of Scope

- Direct verification with government databases
- Legal certification of authenticity
- Real-time video analysis
- Multi-language OCR (English only in current version)
- Mobile application

---

## 8. Constraints

- Single-instance deployment (no horizontal scaling)
- No GPU required — all analysis runs on CPU
- Tesseract OCR used for text extraction (no deep learning OCR)
- Face verification uses histogram-based features (no deep embeddings)
- AI manipulation screening uses frequency-domain heuristics (no trained neural classifiers)

