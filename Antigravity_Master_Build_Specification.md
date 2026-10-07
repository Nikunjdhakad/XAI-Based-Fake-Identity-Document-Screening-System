# AI-Based Fake Identity & Document Screening System
## Antigravity IDE — Master Build Specification

**Project type:** Major Project / Synopsis Implementation  
**Primary backend:** Java Spring Boot  
**AI/Computer Vision service:** Python  
**Frontend:** React  
**Database:** PostgreSQL  
**Deployment:** Docker  
**Architecture:** Modular full-stack + separate AI inference service

---

# 1. Project Vision

Build a web-based document-screening platform that accepts identity documents and certificates and performs multi-signal analysis for possible manipulation.

The system must NOT claim to legally prove that a document is genuine or fake. It is a **screening and risk-assessment system**.

The final output should be:

- Low / Medium / High risk
- reasons supporting the risk
- detected anomalies
- suspicious regions where available
- extracted document fields
- module-level analysis results
- an auditable screening report

The project combines:

1. OCR
2. Image forensics
3. Metadata analysis
4. Identity consistency checking
5. Face verification where a reference face is available
6. AI-manipulation screening
7. Evidence fusion
8. Explainable risk scoring

---

# 2. Source-of-Truth Scope

The approved synopsis defines the following scope.

## Supported document categories

- Identity documents
- Educational certificates
- Experience / training certificates
- Institutional documents
- Selected reports / forms

## Target manipulation types

- Text modification
- Copy-paste manipulation
- Image replacement
- AI-based inpainting
- Digital editing
- Re-saved or re-captured edited files

## Output

Risk score:

- LOW
- MEDIUM
- HIGH

with reasons and suspicious regions.

## Explicitly out of scope

- Direct verification with government databases
- Legal certification of authenticity

The platform must therefore use wording such as:

> "Potentially manipulated", "Suspicious", "Risk assessment", "Screening result"

and NOT:

> "Legally fake", "100% genuine", "Government verified"

unless a future external authority integration actually exists.

---

# 3. Core Product Principle

Do NOT build six independent "fake detectors".

Build six evidence-producing modules.

```text
Document
   |
   +--> OCR ----------------------+
   |
   +--> Image Forensics ----------+
   |
   +--> Metadata Analysis --------+
   |
   +--> Identity Consistency -----+--> Evidence Fusion
   |
   +--> Face Verification --------+
   |
   +--> AI Manipulation ----------+
                                      |
                                      v
                               Risk Score Engine
                                      |
                                      v
                         Explainable Screening Report
```

Each module returns structured evidence.

The final decision is produced by the fusion/risk layer.

---

# 4. Non-Negotiable Architecture

Use a modular architecture with clear ownership.

```text
                    React Frontend
                         |
                         | REST/JSON
                         v
                 Java Spring Boot
                /       |        \
               /        |         \
        PostgreSQL   File Storage  AI Service
                                  |
                                  v
                         Python FastAPI
                                  |
             +--------------------+--------------------+
             |         |          |         |          |
            OCR   Forensics   Metadata  Face/AI   Preprocessing
```

## Service ownership

### React owns

- UI
- navigation
- upload experience
- dashboard
- analysis result visualisation
- suspicious-region viewer
- reports UI
- authentication screens
- user settings

React must NOT contain ML/business scoring logic.

### Spring Boot owns

- authentication
- authorization
- users
- document records
- analysis jobs
- REST APIs
- orchestration
- validation
- evidence storage
- risk-score aggregation
- report metadata
- audit trail
- PostgreSQL access

Spring Boot is the main application backend.

### Python AI service owns

- image preprocessing
- OCR
- image-forensic analysis
- metadata extraction where appropriate
- face detection/embedding comparison
- AI-manipulation model inference
- AI-specific feature extraction

Python must return structured JSON results.

It must NOT directly write to PostgreSQL.

### PostgreSQL owns

Structured persistent data:

- users
- roles
- documents
- document metadata
- extracted fields
- analysis jobs
- module results
- evidence
- risk scores
- reports
- audit records

---

# 5. Recommended Technology Stack

## Frontend

- React
- Vite
- TypeScript
- CSS architecture of choice, preferably CSS Modules or a controlled styling system
- Recharts or equivalent only where charts are genuinely useful

Do NOT automatically use a generic dashboard template.

## Backend

- Java 21 LTS
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Security
- JWT authentication
- PostgreSQL driver
- Lombok only if it improves maintainability
- Maven

## AI Service

- Python 3.11+
- FastAPI
- Uvicorn
- PaddleOCR or Tesseract
- OpenCV
- Pillow
- PyMuPDF and/or appropriate PDF tooling
- scikit-learn
- PyTorch where a deep-learning model is required
- face embedding library/model selected after evaluation

Do not add heavy ML libraries unless the corresponding module actually requires them.

## Infrastructure

- Docker
- Docker Compose for local development

---

# 6. Repository Structure

Use a monorepo.

```text
fake-document-screening/
│
├── frontend/
│   ├── src/
│   ├── public/
│   ├── package.json
│   └── README.md
│
├── backend/
│   ├── src/main/java/
│   ├── src/main/resources/
│   ├── pom.xml
│   └── README.md
│
├── ai-service/
│   ├── app/
│   │   ├── api/
│   │   ├── core/
│   │   ├── schemas/
│   │   ├── services/
│   │   ├── modules/
│   │   │   ├── ocr/
│   │   │   ├── forensics/
│   │   │   ├── metadata/
│   │   │   ├── identity/
│   │   │   ├── face/
│   │   │   └── ai_manipulation/
│   │   └── main.py
│   ├── tests/
│   ├── requirements.txt
│   └── README.md
│
├── database/
│   ├── migrations/
│   └── README.md
│
├── docs/
│   ├── architecture.md
│   ├── api-contract.md
│   ├── database-design.md
│   ├── ai-modules.md
│   ├── ui-ux-spec.md
│   ├── testing-strategy.md
│   └── decisions.md
│
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

Do not create separate unrelated frontend/backend repositories unless explicitly requested later.

---

# 7. API Boundary

The frontend communicates ONLY with Spring Boot.

```text
Frontend
   |
   v
Spring Boot
   |
   v
Python AI Service
```

The frontend must NOT call Python directly.

Spring Boot communicates with Python through REST.

---

# 8. Initial API Design

Do not implement every endpoint immediately. Establish contracts first.

## Authentication

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
```

## Documents

```text
POST /api/documents
GET  /api/documents
GET  /api/documents/{id}
DELETE /api/documents/{id}
```

## Analysis

```text
POST /api/documents/{id}/analysis
GET  /api/documents/{id}/analysis
GET  /api/documents/{id}/analysis/status
```

## Reports

```text
GET /api/documents/{id}/report
GET /api/documents/{id}/report/download
```

The exact contract must be documented before implementation.

---

# 9. AI Service API

Spring Boot sends a document analysis request to Python.

Conceptual endpoint:

```text
POST /internal/analyze
```

Python returns a stable schema.

Example:

```json
{
  "documentId": "uuid",
  "status": "COMPLETED",
  "modules": {
    "ocr": {},
    "forensics": {},
    "metadata": {},
    "identityConsistency": {},
    "faceVerification": {},
    "aiManipulation": {}
  },
  "evidence": [],
  "suspiciousRegions": [],
  "processingTimeMs": 0
}
```

The exact fields should be versioned and documented.

Never make Spring Boot depend on undocumented Python response fields.

---

# 10. Module Specifications

## 10.1 OCR

Purpose:

Convert document images/PDF pages into machine-readable text.

Candidate technology:

- PaddleOCR
- Tesseract

Output should include:

- extracted text
- bounding boxes where available
- confidence values
- detected fields where possible

Example:

```json
{
  "text": "Nikunj Dhakad",
  "field": "name",
  "confidence": 0.96,
  "bbox": [x1, y1, x2, y2]
}
```

OCR is evidence, not ground truth.

---

# 11. Image Forensics

Purpose:

Identify image-level evidence that may indicate manipulation.

Possible techniques:

- image normalization
- compression analysis
- noise inconsistency
- copy-move indicators
- edge/texture anomalies
- model-based manipulation detection

OpenCV should be used for preprocessing and image analysis.

Do not claim that OpenCV alone proves forgery.

Output:

```json
{
  "riskScore": 0.0,
  "findings": [],
  "suspiciousRegions": []
}
```

The exact algorithms should be selected and documented after baseline experimentation.

---

# 12. Metadata Analysis

Purpose:

Extract file-level metadata where available.

Potential information:

- file type
- dimensions
- timestamps
- software/producer information
- PDF metadata
- image EXIF information

Metadata may be missing or altered.

Therefore:

> Metadata is supporting evidence, never definitive proof.

Output:

```json
{
  "available": true,
  "anomalies": [],
  "evidenceScore": 0.0
}
```

---

# 13. Identity Consistency

Purpose:

Check whether extracted document information is internally consistent.

Examples:

- date relationships
- field formats
- document-number patterns
- required fields
- repeated name inconsistencies
- impossible or contradictory values

This is primarily business/rule logic.

Do not connect to government databases.

Output:

```json
{
  "consistent": false,
  "checks": [
    {
      "rule": "example-rule",
      "passed": false,
      "message": "..."
    }
  ]
}
```

---

# 14. Face Verification

Purpose:

Compare a document face against a reference face when a reference image is available.

Pipeline:

```text
Document
   |
Face Detection
   |
Face Crop
   |
Embedding Model
   |
Similarity Comparison
   |
Match / Possible Mismatch
```

Important:

- A reference face may be optional.
- If no reference image exists, mark the module as NOT_APPLICABLE.
- Do not claim legal identity verification.

Output:

```json
{
  "status": "MATCH | MISMATCH | NOT_APPLICABLE",
  "similarity": 0.0
}
```

Thresholds must be configurable and validated experimentally.

---

# 15. AI Manipulation Screening

Purpose:

Screen for possible AI-assisted manipulation, especially operations such as inpainting.

Important:

Do not invent a model.

First:

1. Identify suitable open/research models.
2. Check license.
3. Check input/output requirements.
4. Check whether the model supports document manipulation.
5. Test on available datasets.
6. Select a baseline.
7. Document limitations.

Potential output:

```json
{
  "riskScore": 0.0,
  "manipulationType": "UNKNOWN",
  "confidence": 0.0,
  "suspiciousRegions": []
}
```

Do not state that AI detection is 100% reliable.

---

# 16. Evidence Fusion

This is one of the main application-level contributions.

Each module produces evidence.

Example:

```text
OCR                  → low suspicion
Forensics            → high suspicion
Metadata             → medium suspicion
Identity consistency → low suspicion
Face verification    → match
AI manipulation      → high suspicion
```

The fusion layer combines these results.

Do NOT hard-code arbitrary weights before validation.

Start with a transparent baseline.

For example:

- normalized module scores
- configurable weights
- missing modules handled explicitly
- confidence-aware aggregation

Store the fusion configuration/version so results remain auditable.

---

# 17. Risk Score

Final output:

```text
LOW
MEDIUM
HIGH
```

The system must also store:

- numeric score
- risk category
- score version
- module contributions
- explanation

The thresholds must be configurable.

Example conceptual structure:

```json
{
  "score": 0.74,
  "level": "HIGH",
  "explanation": [
    "Suspicious visual region detected",
    "AI manipulation screening returned elevated risk"
  ]
}
```

Do not present arbitrary thresholds as scientifically validated until experiments support them.

---

# 18. Suspicious Regions

Where a module can localize evidence, store:

```json
{
  "x": 120,
  "y": 80,
  "width": 200,
  "height": 70,
  "source": "FORENSICS",
  "confidence": 0.81
}
```

The frontend should overlay these regions on the document.

Use a clear legend:

- module
- severity
- confidence
- explanation

---

# 19. UI/UX — IMPORTANT

The interface must NOT look like a generic AI-generated dashboard.

Avoid:

- excessive glassmorphism
- purple/blue AI gradients everywhere
- huge glowing cards
- generic "AI-powered" hero sections
- excessive rounded cards
- random animations
- dashboard templates copied from common AI products
- excessive statistics that have no purpose

## Design direction

Create a product that feels like a professional **digital document forensics workstation**.

Visual inspiration should come from:

- forensic analysis software
- document review systems
- enterprise security products
- professional audit tools
- modern editorial information design

Do not copy any one product.

---

# 20. UI Design Principles

## Color

Use a restrained palette.

Suggested conceptual system:

- neutral background
- high-contrast text
- one primary brand color
- amber for warnings
- red for high risk
- green for low risk
- blue only for informational elements

Do not make the whole interface colorful.

## Typography

Use a professional modern sans-serif.

Use strong hierarchy:

- page title
- section title
- body
- metadata
- labels

Avoid oversized marketing typography inside the application.

## Layout

Prioritize information density without clutter.

Use:

- split panes
- document preview
- evidence timeline
- analysis status
- module results
- suspicious-region viewer

---

# 21. Main Screens

## A. Login

Minimal, professional.

## B. Dashboard

Show:

- total screened documents
- recent screenings
- risk distribution
- processing status
- quick upload

Do not overload the dashboard with meaningless charts.

## C. New Screening

The upload page should feel like a document-analysis workspace.

Include:

- drag/drop
- file picker
- supported format information
- optional reference face
- document type
- analysis settings

## D. Analysis Progress

Show actual stages:

```text
Upload
✓
Preprocessing
✓
OCR
✓
Image Forensics
...
AI Screening
...
Evidence Fusion
...
Report
```

Do not use a fake percentage if the backend cannot provide real progress.

## E. Results

This is the most important page.

Recommended layout:

```text
--------------------------------------------------
Risk: HIGH                    Score: 74/100
--------------------------------------------------

Document Preview        Evidence Summary

[document]              OCR          LOW
                        Forensics    HIGH
                        Metadata     MEDIUM
                        Identity     LOW
                        Face         MATCH
                        AI Screen    HIGH

--------------------------------------------------
Suspicious Regions
[highlighted document]

--------------------------------------------------
Why this result?
• ...
• ...
• ...

--------------------------------------------------
Detailed Analysis
[expandable module sections]

                    Download Report
--------------------------------------------------
```

## F. History

Search/filter by:

- date
- document type
- risk
- status

## G. Document Details

Show complete audit trail.

---

# 22. UX Behavior

The UI should communicate uncertainty.

Use wording:

- "Potential manipulation detected"
- "Elevated risk"
- "Evidence found"
- "Module unavailable"
- "Insufficient image quality"

Do not say:

- "Definitely fake"
- "100% AI generated"
- "Authenticity guaranteed"

---

# 23. Accessibility

Implement:

- keyboard navigation
- readable contrast
- visible focus states
- semantic buttons
- meaningful error messages
- responsive layout
- mobile-friendly document viewing

---

# 24. Database Design

Initial entities:

```text
User
Role
Document
DocumentFile
AnalysisJob
OcrResult
ForensicResult
MetadataResult
IdentityConsistencyResult
FaceVerificationResult
AiManipulationResult
Evidence
SuspiciousRegion
RiskAssessment
Report
AuditLog
```

Use UUIDs for public identifiers.

Store timestamps.

Use relationships rather than duplicating data.

Do not store passwords in plaintext.

---

# 25. Security

Use Spring Security + JWT.

Requirements:

- password hashing
- JWT authentication
- authorization
- input validation
- upload validation
- file-size limits
- MIME/type validation
- safe file naming
- path traversal protection
- CORS configuration
- internal AI-service authentication
- audit logging

Never expose the Python AI service directly to the public frontend.

---

# 26. File Handling

Accepted formats should be explicitly configured.

Initially consider:

- PDF
- JPG/JPEG
- PNG

The backend should:

1. validate extension
2. validate MIME type
3. enforce size limits
4. assign safe internal filename
5. store securely
6. create database record
7. send analysis request

Never trust the uploaded filename.

---

# 27. Analysis Job Model

Analysis may take time.

Use a job-based flow.

```text
UPLOAD
   ↓
QUEUED
   ↓
PROCESSING
   ↓
COMPLETED
```

Failure:

```text
FAILED
```

Do not freeze the browser waiting for a long AI request.

The UI should poll or use a future real-time mechanism.

For the first implementation, simple polling is acceptable.

---

# 28. Error Handling

Every module must be allowed to fail independently.

Example:

```text
OCR             SUCCESS
Forensics       SUCCESS
Metadata        SUCCESS
Identity        SUCCESS
Face            NOT_APPLICABLE
AI Screening    FAILED
```

The entire document should NOT automatically become FAILED.

The fusion layer must understand module states.

---

# 29. Testing Strategy

## Backend

- unit tests
- service tests
- controller tests
- security tests
- validation tests

## AI service

- module unit tests
- schema tests
- image fixture tests
- failure tests

## Integration

Test:

```text
React → Spring Boot → Python → Spring Boot → PostgreSQL
```

## ML evaluation

Use:

- Accuracy
- Precision
- Recall
- F1
- ROC-AUC
- IoU where localization is evaluated

Also evaluate robustness using:

- JPEG compression
- blur
- resizing
- screenshots
- mobile-camera simulation

And evaluate generalisation using unseen manipulation techniques where dataset design permits.

---

# 30. Development Phases

DO NOT implement the entire project in one generation.

## Phase 0 — Specification

Create:

- architecture document
- API contract
- database ER design
- UI/UX specification
- environment specification
- decision log

No large feature implementation yet.

## Phase 1 — Repository Foundation

Create:

- monorepo
- React app
- Spring Boot app
- Python FastAPI app
- PostgreSQL
- Docker Compose
- environment configuration
- health endpoints

Goal:

All services start successfully.

## Phase 2 — Backend Foundation

Implement:

- User
- authentication
- JWT
- roles
- document entity
- upload API
- PostgreSQL integration
- audit basics

Goal:

A user can securely upload a document.

## Phase 3 — Frontend Foundation

Build the complete visual shell:

- login
- dashboard
- upload
- analysis
- results
- history
- document details

Focus strongly on unique UI/UX.

Goal:

Professional product interface before adding complex AI.

## Phase 4 — OCR

Implement Python OCR module.

Connect:

```text
Spring Boot → Python → OCR → Spring Boot
```

Add tests.

## Phase 5 — Metadata

Implement metadata extraction.

Add result storage.

## Phase 6 — Identity Consistency

Implement deterministic validation rules.

## Phase 7 — Image Forensics Baseline

Start with explainable baseline techniques.

Do not immediately add an enormous model.

## Phase 8 — Face Verification

Add:

- face detection
- embeddings
- similarity
- threshold configuration
- NOT_APPLICABLE state

## Phase 9 — AI Manipulation Screening

Research/select a suitable model.

Document:

- source
- license
- dataset
- input format
- limitations
- benchmark performance

Then integrate.

## Phase 10 — Evidence Fusion

Combine module outputs.

Create:

- score
- risk category
- explanation
- module contribution

## Phase 11 — Suspicious Region Viewer

Overlay available regions on the document.

## Phase 12 — Reports

Create downloadable screening report.

## Phase 13 — Evaluation

Run experiments.

Compare:

- individual modules
- combined approach
- robustness
- unseen manipulation performance

## Phase 14 — Hardening

Security, validation, error handling, performance, UX polish.

## Phase 15 — Final Documentation

Prepare:

- SRS
- architecture
- database
- API documentation
- testing report
- experiment results
- limitations
- future scope
- viva explanation

---

# 31. Antigravity Execution Rules

These rules are mandatory.

### Rule 1
Work on ONE phase at a time.

### Rule 2
Before modifying architecture, read the existing documentation.

### Rule 3
Do not silently change the technology stack.

### Rule 4
Do not introduce a second framework that duplicates an existing responsibility.

### Rule 5
Do not rewrite working modules without a reason.

### Rule 6
After every phase:

1. run tests
2. run build
3. verify service startup
4. update documentation
5. record decisions

### Rule 7
Never create fake/mock AI results in production code and present them as real analysis.

Mocks are allowed only in clearly marked development/test mode.

### Rule 8
Never invent a research model or dataset.

### Rule 9
When a model/library is proposed, verify:

- license
- compatibility
- maintenance
- input/output requirements

### Rule 10
If a requirement is ambiguous, STOP and document the ambiguity rather than making a large architectural assumption.

### Rule 11
Do not modify unrelated files.

### Rule 12
Keep commits/changes logically grouped by phase.

---

# 32. Definition of Done

A phase is complete only when:

- implementation exists
- tests pass
- build succeeds
- API contract is documented
- errors are handled
- UI works where applicable
- no unrelated regressions exist
- documentation is updated

Do not move to the next phase if the current phase is broken.

---

# 33. First Milestone

The first implementation milestone must NOT attempt OCR, face recognition or AI detection.

First create the foundation:

```text
React
   ↕
Spring Boot
   ↕
PostgreSQL

Spring Boot
   ↕
Python FastAPI
```

Create health checks:

```text
GET /api/health
GET /internal/health
```

Confirm:

- frontend starts
- backend starts
- Python service starts
- database starts
- backend connects to database
- backend can call Python
- environment variables work
- Docker Compose works

Then stop and verify.

---

# 34. Initial UI Direction

Brand the product as a serious forensic analysis platform.

Temporary working product name:

**VeriLens**

Possible tagline:

**"Evidence-first document screening."**

The name can be changed later.

The interface should communicate:

> upload → inspect → understand → decide

rather than:

> upload → AI magic → fake

Use subtle motion only when it communicates system state.

Examples:

- upload progress
- analysis stage transitions
- region highlighting
- expandable evidence
- report generation

Avoid decorative animations.

---

# 35. Documentation Antigravity Must Maintain

Maintain these files throughout development:

```text
docs/
├── architecture.md
├── api-contract.md
├── database-design.md
├── ai-modules.md
├── ui-ux-spec.md
├── testing-strategy.md
├── decisions.md
└── implementation-status.md
```

`implementation-status.md` should contain:

```text
Phase 0  DONE
Phase 1  IN PROGRESS
Phase 2  NOT STARTED
...
```

---

# 36. Final Product Definition

The finished system should allow a user to:

1. Register/login
2. Upload a document
3. Optionally provide a reference face
4. Select document type
5. Start screening
6. See actual analysis progress
7. View extracted text
8. View metadata findings
9. View forensic findings
10. View identity consistency checks
11. View face verification result when applicable
12. View AI-manipulation screening result
13. View suspicious regions
14. View final risk score
15. Read explanations
16. Download a screening report
17. View previous screenings
18. Review audit information

---

# 37. Important Academic Positioning

The project should be presented as:

> **An explainable, multi-signal AI-assisted document screening and risk-assessment platform.**

Not:

> A perfect fake-document detector.

The project must openly acknowledge:

- no 100% detection guarantee
- AI detectors can fail
- metadata can be missing/manipulated
- OCR can contain errors
- face verification is a similarity signal
- unseen forgeries are difficult
- degraded images reduce performance
- legal authenticity is outside project scope

These limitations make the project more credible, not weaker.

---

# 38. Immediate Task for Antigravity

When this document is first provided, DO NOT implement the whole project.

Only perform Phase 0:

1. Inspect the workspace.
2. Create the monorepo directory structure.
3. Create the documentation files.
4. Write the architecture specification.
5. Write the API contract draft.
6. Write the database design draft.
7. Write the UI/UX specification.
8. Write the AI-module specification.
9. Write the testing strategy.
10. Create `implementation-status.md`.
11. Create `.env.example`.
12. Create a root README.
13. Do NOT implement complex AI models.
14. Do NOT create placeholder fake results.
15. Do NOT generate the complete frontend yet.
16. Do NOT install unnecessary dependencies.
17. Stop after Phase 0 and report exactly what was created.

After Phase 0 is reviewed, proceed to Phase 1 only.

---

# 39. Conflict Prevention Rule

Before every future task, Antigravity must answer internally:

- Which phase am I in?
- Which service owns this feature?
- Which existing files will be modified?
- Does this change affect an existing API contract?
- Does this change affect the database schema?
- Does this change affect another service?
- Are tests required?
- Is documentation required?

If yes, update the affected documentation before implementation.

Never solve a local problem by changing the global architecture without documenting the decision.

---

# 40. Success Criteria

The project succeeds when it demonstrates:

- working Java Spring Boot backend
- working React application
- working Python AI service
- PostgreSQL persistence
- real document upload
- real OCR
- real metadata analysis
- real identity consistency checks
- real face verification where applicable
- a defensible AI-manipulation screening baseline
- evidence fusion
- explainable risk assessment
- suspicious-region visualisation where supported
- downloadable report
- security controls
- automated tests
- documented evaluation
- professional, distinctive UI
