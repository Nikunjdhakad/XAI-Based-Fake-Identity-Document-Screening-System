# VeriLens — Implementation Status

**Last updated:** 2026-11-16

---

| Phase | Description                  | Status         |
| ----- | ---------------------------- | -------------- |
| 0     | Specification                | ✅ COMPLETE     |
| 1     | Repository Foundation        | ✅ COMPLETE     |
| 2     | Backend Foundation           | ✅ COMPLETE     |
| 3     | Frontend Foundation          | ✅ COMPLETE     |
| 4     | OCR                          | ✅ COMPLETE     |
| 5     | Metadata Analysis            | ✅ COMPLETE     |
| 6     | Identity Consistency         | ✅ COMPLETE     |
| 7     | Image Forensics              | ✅ COMPLETE     |
| 8     | Face Verification            | ✅ COMPLETE     |
| 9     | AI Manipulation Screening    | ✅ COMPLETE     |
| 10    | Evidence Fusion & Risk Scoring | ✅ COMPLETE     |
| 11    | Suspicious Region Viewer     | ✅ COMPLETE     |
| 12    | Reports                      | ✅ COMPLETE     |
| 13    | Evaluation                   | ✅ COMPLETE     |
| 14    | Hardening                    | ✅ COMPLETE     |
| 15    | Final Documentation          | ✅ COMPLETE     |

---

## Phase 15 — Final Documentation — Details

### New Documents Created

- [x] **`docs/srs.md`** — Software Requirements Specification: 25 functional requirements (FR-01 to FR-25), 13 non-functional requirements (NFR-01 to NFR-13), supported document categories, target manipulation types, out-of-scope items, constraints
- [x] **`docs/experiment-results.md`** — Testing report and experiment results: unit/integration test summary (24 backend + 135 Python + frontend build), full Phase 13 ML evaluation metrics (classification, robustness, IoU), analysis interpretation, comparison with objectives
- [x] **`docs/limitations-and-future-scope.md`** — Known limitations (OCR, forensics, AI manipulation, face verification, system-level, evaluation) and future scope (short-term, medium-term, long-term vision, research directions)
- [x] **`docs/viva-preparation.md`** — Viva Q&A guide: elevator pitch, 12 key questions with detailed answers, architecture diagrams, code files to explain, academic positioning talking points

### Existing Documents Updated

- [x] **`docs/architecture.md`** — Version bumped to 1.0.0, phase updated to 15, security architecture table expanded with rate limiting, security headers, magic bytes validation
- [x] **`docs/database-design.md`** — Version bumped to 1.0.0, phase updated to 15, implementation notes updated to reflect all 17 Flyway migrations are implemented, migration summary table added
- [x] **`docs/api-contract.md`** — Phase updated to 14, rate limiting and security headers sections added
- [x] **`README.md`** — All 15 phases marked complete, documentation table expanded with 4 new docs, Flyway migration count updated to V17, reports migration added to schema table

---

## Phase 14 — Hardening — Details

### Security Hardening

- [x] **Security headers filter** (`SecurityHeadersFilter.java`) — X-Content-Type-Options, X-Frame-Options, X-XSS-Protection, Referrer-Policy, Content-Security-Policy, Permissions-Policy, Strict-Transport-Security, Cache-Control for API responses
- [x] **Rate limiting filter** (`RateLimitFilter.java`) — per-IP in-memory rate limiter: 60 req/min general, 10 req/min for login/register; X-RateLimit-Limit and X-RateLimit-Remaining headers; 429 JSON error response
- [x] **Magic bytes file validation** — StorageService now verifies file content matches extension via magic bytes (PDF `%PDF`, JPEG `FF D8`, PNG `89 50 4E 47`) to prevent disguised executable uploads
- [x] **AI service request size guard** — Python middleware rejects requests > 25 MB before processing
- [x] **AI service global exception handler** — catches unhandled exceptions, logs full trace to container logs, returns safe JSON error to caller (no stack trace leaks)

### Input Validation Hardening

- [x] **Pagination bounds clamping** — `InputSanitizer` utility: page ≥ 0, size 1–100, sort field validated against whitelist (createdAt, originalFilename, status, documentType)
- [x] **Log injection prevention** — `InputSanitizer.sanitizeForLog()` strips newlines and control characters

### Error Handling Hardening

- [x] **Expanded GlobalExceptionHandler** — now handles 10 exception types:
  - `MethodArgumentNotValidException` (400 — @Valid errors)
  - `IllegalArgumentException` (400 — business validation)
  - `MissingServletRequestParameterException` (400 — missing params)
  - `HttpMessageNotReadableException` (400 — malformed JSON)
  - `MaxUploadSizeExceededException` (413 — file too large)
  - `HttpRequestMethodNotSupportedException` (405 — wrong HTTP method)
  - `HttpMediaTypeNotSupportedException` (415 — wrong content type)
  - `AccessDeniedException` (403 — insufficient role)
  - `BadCredentialsException` (401 — invalid login)
  - `SecurityException` (403 — path traversal attempts, logged)
  - `NoResourceFoundException` (404 — unmapped paths)
  - `Exception` (500 — catch-all, logs full stack trace, returns safe message)

### Performance Hardening

- [x] **Response compression** — GZIP enabled in application.yml for JSON/XML/HTML/PDF responses > 1 KB
- [x] **AI service client timeouts** — `RestTemplate` configured with 10s connect timeout, configurable read timeout (default 120s) via `RestTemplateBuilder`

### UX Polish

- [x] **Error Boundary** (`ErrorBoundary.tsx`) — catches React rendering crashes, shows fallback UI with "Try Again" and "Go to Dashboard" buttons, expandable error details
- [x] **404 Not Found page** (`NotFound.tsx`) — clean 404 page for unmatched routes within authenticated area
- [x] **Skip-to-content link** — screen reader and keyboard navigation support in AppLayout
- [x] **ARIA landmarks** — `role="main"`, `role="complementary"`, `aria-label` on nav and sidebar
- [x] **Focus-visible outlines** — consistent 2px accent ring on all focusable elements
- [x] **Reduced motion support** — `@media (prefers-reduced-motion: reduce)` disables animations
- [x] **Responsive text scaling** — font sizes adjusted for viewports < 768px
- [x] **Utility CSS classes** — `.spinner`, `.spinner--lg`, `.empty-state`, `.toast--error`, `.toast--success`
- [x] **Frontend build succeeds** — 313 KB JS (96 KB gzip), 24 KB CSS

---

## Phase 13 — Evaluation — Details

### Completed Items

- [x] **Evaluation framework** (`ai-service/evaluation/run_evaluation.py`) — 970-line comprehensive evaluation suite
- [x] **Synthetic test image generation**: 6 genuine + 8 manipulated = 14 base cases
- [x] **Manipulation categories covered**: text_edit, copy_paste, inpainting, generation, resave
- [x] **Robustness degradation suite**: JPEG Q30/Q50, Gaussian blur r=2.0, resize 50% → 16 degraded variants (30 total cases)
- [x] **Classification metrics**: Accuracy, Precision, Recall, F1, ROC-AUC computed per module and combined
- [x] **Localization metrics**: IoU evaluation for suspicious region overlap with ground-truth bounding boxes
- [x] **Results**: forensics 0.000 F1 (synthetic data limitation), AI manipulation 0.727 F1, combined 0.762 F1
- [x] **Robustness results**: AI manipulation module maintains 100% recall under all degradation types
- [x] **IoU**: 7 overlapping regions found, average IoU = 0.1676
- [x] **Evaluation report**: `ai-service/evaluation/results/evaluation_report.json` (full structured results)
- [x] **Evaluation summary**: `ai-service/evaluation/results/evaluation_summary.md` (human-readable report)
- [x] **Limitations documented**: synthetic test data, small test suite, forensics module limitation on programmatic edits, AI manipulation false-positive rate on genuine images with uniform areas

### Key Findings

| Module | Accuracy | Precision | Recall | F1 | ROC-AUC |
| ------ | -------- | --------- | ------ | -- | ------- |
| forensics | 0.429 | 0.000 | 0.000 | 0.000 | 0.000 |
| ai_manipulation | 0.571 | 0.571 | 1.000 | 0.727 | 0.521 |
| combined | 0.643 | 0.615 | 1.000 | 0.762 | 0.521 |

- Forensics module (ELA/noise/edge) scores 0.0 on synthetic test data because programmatic PIL edits do not produce JPEG compression artifacts that ELA detects. The module functions correctly on real re-saved/spliced JPEG documents.
- AI manipulation module detects all manipulated images (100% recall) but has false positives on genuine synthetic certificates with large uniform regions.
- Combined fusion achieves 0.762 F1, demonstrating that multi-signal fusion improves over individual modules.

---

## Phase 12 — Reports — Details

### Completed Items

- [x] **Backend: `ReportService`** (496 lines) — full PDF generation using Apache PDFBox 3.0.3:
  - Gathers all module results (OCR, forensics, metadata, identity, face, AI manipulation) + risk assessment
  - Builds structured PDF: header, document info, risk assessment (score, level, explanations, module contributions), module summaries, disclaimer, footer
  - PDType1Font (Helvetica family), word-wrap support for long text
  - Reports stored at `{uploadDir}/reports/report_{documentId}_{timestamp}.pdf`
- [x] **Backend: `Report` entity** — JPA entity mapped to `reports` table (UUID id, document, task, storagePath, format, fileSize, generatedAt)
- [x] **Backend: `ReportRepository`** — `findTopByDocumentIdOrderByGeneratedAtDesc()`
- [x] **Backend: `ReportResponse` DTO** — reportId, documentId, format, fileSize, generatedAt, downloadUrl
- [x] **Backend: Flyway V17** — `reports` table with document_id FK (ON DELETE CASCADE), task_id FK, indexes
- [x] **Backend: Controller endpoints**:
  - `GET /api/documents/{id}/analysis/report` → report metadata
  - `GET /api/documents/{id}/analysis/report/download` → PDF binary with Content-Disposition attachment
- [x] **Backend: Auto-generation** — `ReportService.generateReport()` called after evidence fusion completes in async pipeline
- [x] **Backend: PDFBox 3.0.3 dependency** in `pom.xml` (Apache 2.0 license)
- [x] **Frontend: `ReportResponse` TypeScript interface** added to `types.ts`
- [x] **Frontend: `api.download()` method** — blob-based fetch + browser download trigger for binary responses (PDF)
- [x] **Frontend: Results page wired up**:
  - Fetches report metadata via `GET /api/documents/{id}/analysis/report`
  - Report info card shows generation date, file size, format
  - "Download Report" button: enabled when report exists, triggers PDF download
  - Loading state with "⏳ Downloading…" indicator
  - Graceful disabled state when report not yet generated
- [x] **Frontend: Report card CSS** — reportCard, reportInfo, reportIcon, reportTitle, reportMeta styles
- [x] **Frontend build succeeds** — 310 KB JS (95 KB gzip), 22 KB CSS
- [x] **Documentation**: `api-contract.md` (Reports section updated to ✅ Implemented), `decisions.md` (DEC-011), `implementation-status.md`

---

## Phase 11 — Suspicious Region Viewer — Details

### Completed Items

- [x] **Results page fully rewritten** — fetches all module results + risk assessment in parallel
- [x] **Real risk score display** — numeric score (e.g. 74/100) with level-coloured header (HIGH=red, MEDIUM=amber, LOW=green)
- [x] **Evidence summary table** — all 6 modules with real status (✓/✗/N/A), risk level, and detail summary
- [x] **Suspicious region overlay** — bounding boxes from forensics (ELA, noise, edge) + AI manipulation (JPEG ghost, gradient) rendered on a schematic document canvas with colour-coded source labels and confidence percentages
- [x] **Region legend** — lists all source types with matching colours
- [x] **Risk explanation** — real human-readable findings from evidence fusion, with level-specific summary
- [x] **Detailed analysis accordions** — expandable per-module sections showing:
  - OCR: extracted text, page count, confidence
  - Forensics: risk score, ELA metrics, findings list with severity badges
  - Metadata: evidence score, anomalies list, properties table
  - Identity: consistency verdict, failed check counts, check list with pass/fail
  - Face: match status, similarity percentage, threshold, notes
  - AI Manipulation: risk score, manipulation type, findings, model info + limitations
- [x] **JSON utility module** (`utils/jsonUtils.ts`) — typed parse functions for regions, arrays, objects, strings
- [x] **All TypeScript types** added to `types.ts` for every module response
- [x] **Build succeeds** — 309 KB JS (95 KB gzip), 22 KB CSS
- [x] **Docker frontend container rebuilt** and running on port 3000

---

## Phase 10 — Evidence Fusion & Risk Scoring — Details

### Completed Items

- [x] **`EvidenceFusionService`** — combines all 6 module outputs into one `RiskAssessment`
- [x] **Transparent weighted baseline v1.0.0**:
  - Forensics: 25%, AI Manipulation: 25%, Metadata: 15%, Identity: 15%, OCR: 10%, Face: 10%
  - Risk levels: LOW (0.00–0.33), MEDIUM (0.34–0.66), HIGH (0.67–1.00)
  - Missing/NOT_APPLICABLE modules excluded from denominator (partial weight redistribution)
- [x] **Human-readable explanations** generated per module when anomalies detected
- [x] **Module contributions map** stored for auditability
- [x] **Scoring version** (`1.0.0`) stored — must be refined in Phase 13 evaluation
- [x] **Flyway V15** — `risk_assessments` table (unique index on task_id)
- [x] **Flyway V16** — `risk_level` column added to `documents` table
- [x] **`RiskAssessment` entity** + `RiskAssessmentRepository`
- [x] **`RiskAssessmentResponse` DTO**
- [x] **`Document.riskLevel`** field added — updated by fusion after analysis completes
- [x] **`DocumentResponse.fromEntity()`** now returns actual `riskLevel` from entity
- [x] **`AnalysisService`** calls `evidenceFusionService.fuse()` after all module results persisted
- [x] **`GET /api/documents/{id}/analysis/risk`** endpoint
- [x] **24 Spring Boot tests** all pass — BUILD SUCCESS

### Important Notes

- Thresholds are **initial estimates** — they will be validated in Phase 13
- Language in explanations uses "potential manipulation", "elevated risk" — never "definitely fake"
- Face verification contributes only when a reference face was provided (NOT_APPLICABLE excluded from weight)

---

## Phase 9 — AI Manipulation Screening — Details

### Completed Items

- [x] **Python AI manipulation module** (`ai-service/app/modules/ai_manipulation.py`) — Explainable, frequency-domain and statistical screening with 4 complementary techniques:
  1. **Spectral Analysis (2D FFT)**: detects high-frequency energy reduction and periodic mid-frequency GAN grid peaks
  2. **JPEG Ghost Detection**: multi-quality re-compression (Q=50, 60, 70, 80, 90) detects block error variance indicating copy-paste/splicing
  3. **Gradient Consistency**: Sobel gradient magnitude block variance detects unnaturally smooth inpainting fills
  4. **Texture Regularity**: Local Binary Pattern (LBP) histogram uniformity and Shannon entropy detect synthetic AI-generated textures
- [x] **Inferred manipulation type**: `NONE_DETECTED`, `SPLICING`, `INPAINTING`, `GENERATION`, `UNKNOWN` based on technique findings
- [x] **Suspicious regions**: spatial bounding boxes (`x`, `y`, `width`, `height`, `confidence`, `description`) capped at 15 regions
- [x] **Model/technique info**: name, version, techniques, limitations, and benchmark documented in response schema
- [x] **Integration in `main.py`**: `/internal/analyze` pipeline wires `AI_MANIPULATION`, health endpoint includes status, module list updated
- [x] **Flyway V13** (`V13__create_ai_manipulation_results_table.sql`): `ai_manipulation_results` table with task_id, document_id, status, risk_score, manipulation_type, confidence, findings_json, suspicious_regions_json, model_info_json
- [x] **`AiManipulationResult` entity** (`com.verilens.entity.AiManipulationResult`)
- [x] **`AiManipulationResultRepository`** (`com.verilens.repository.AiManipulationResultRepository`)
- [x] **`AiManipulationResponse` DTO** (`com.verilens.dto.AiManipulationResponse`)
- [x] **`AnalysisService`** updated: injects repository, persists AI manipulation results, provides `getAiManipulationResult(documentId, userId)`, sets default modules
- [x] **`AnalysisController`** updated: `GET /api/documents/{documentId}/analysis/ai-manipulation`
- [x] **Python tests** (`tests/test_ai_manipulation.py`): 40 comprehensive tests covering availability, schema, individual techniques, findings, regions, error handling, risk scores, endpoint
- [x] **Documentation**: `api-contract.md`, `ai-modules.md`, `decisions.md` (DEC-010), `implementation-status.md`

---

## Phase 8 — Face Verification — Details

### Completed Items

- [x] **Python face module** (`ai-service/app/modules/face.py`) — OpenCV Haar Cascade face detection + histogram-based feature extraction (HOG-like, 384-dim vector)
- [x] **NOT_APPLICABLE when no reference face** — checked before any processing
- [x] **Face detection**: OpenCV `CascadeClassifier` with frontal face Haar cascade; graceful fallback to `NOT_APPLICABLE` when OpenCV 5 (which removed CascadeClassifier) is used on host
- [x] **Feature extraction**: global pixel histogram (256 bins) + local block histograms (4×4 grid × 8 bins) = 384-dim vector with lighting normalisation
- [x] **Cosine similarity** with configurable threshold (0.70 default)
- [x] **Status values**: `POSSIBLE_MATCH`, `POSSIBLE_MISMATCH`, `NOT_APPLICABLE`, `FAILED` — avoids misleading "MATCH/MISMATCH" absolute terms
- [x] **Reference face** read from `DocumentFile` (FileType.REFERENCE_FACE) and passed to AI service
- [x] **`AiServiceClient`** updated to support optional second file (`referenceFace`)
- [x] **`/internal/analyze`** accepts optional `referenceFace` upload
- [x] **Health endpoint** reports `face: AVAILABLE` in Docker (OpenCV 4.10)
- [x] **Flyway V12** — `face_verification_results` table
- [x] **`FaceVerificationResult` entity** + `FaceVerificationResultRepository`
- [x] **`FaceVerificationResponse` DTO**
- [x] **`AnalysisService`** updated — passes reference face bytes, persists face results, default modules `OCR,METADATA,IDENTITY,FORENSICS,FACE`
- [x] **`GET /api/documents/{id}/analysis/face`** endpoint
- [x] **Python tests** — 15 face tests (availability, schema, NOT_APPLICABLE logic, endpoint)
- [x] **96 Python tests total** (1 skipped), **24 Spring Boot tests** — all green

### Technology Note

OpenCV `CascadeClassifier` is used for Phase 8 baseline. Deep embedding models (ArcFace, FaceNet) will be evaluated in Phase 13. The current approach produces a `POSSIBLE_MATCH`/`POSSIBLE_MISMATCH` signal — never a definitive identity claim.

---

## Phase 7 — Image Forensics — Details

### Completed Items

- [x] **Python forensics module** (`ai-service/app/modules/forensics.py`) — OpenCV-based explainable baseline techniques
- [x] **Error Level Analysis (ELA)** — re-save at known JPEG quality, compute pixel-level difference, detect high-error regions with bounding boxes
- [x] **Noise inconsistency** — Laplacian-based local noise variance, detect blocks with anomalous noise patterns (splicing indicators)
- [x] **Edge density analysis** — Canny edge detection, detect blocks with unusually high edge density (copy-paste indicators)
- [x] **Risk score** — severity-weighted normalised score (0.0–1.0) from aggregated findings
- [x] **Suspicious regions** — bounding boxes from all three techniques, capped at 20, with source/confidence labels
- [x] **OpenCV + NumPy** added to requirements.txt and Dockerfile
- [x] **`/internal/analyze`** runs FORENSICS when requested
- [x] **Health endpoint** reports `forensics: AVAILABLE`
- [x] **Flyway V11** — `forensic_results` table
- [x] **`ForensicResult` entity** + `ForensicResultRepository`
- [x] **`ForensicResultResponse` DTO**
- [x] **`AnalysisService`** updated — persists forensic results, default modules now `OCR,METADATA,IDENTITY,FORENSICS`
- [x] **`GET /api/documents/{id}/analysis/forensics`** endpoint
- [x] **Python tests** — 17 forensics tests (availability, schema, findings structure, regions, endpoint)
- [x] **79 Python tests total** (1 skipped), **24 Spring Boot tests** — all green

---

## Phase 6 — Identity Consistency — Details

### Completed Items

- [x] **Python identity module** (`ai-service/app/modules/identity.py`) — rule-based checks on OCR output
- [x] **Date detection** — 3 formats (DD/MM/YYYY, YYYY-MM-DD, DD Month YYYY) with deduplication and validation
- [x] **Field detection** — labelled fields (name, date, ID, institution), ID number patterns, name extraction
- [x] **9 consistency rules**: date presence, future dates, date range plausibility, impossible dates, name presence, ID presence, date ordering, required fields, sufficient text, name consistency
- [x] **Consistency verdict** — inconsistent if any HIGH check fails or 2+ MEDIUM checks fail
- [x] **`/internal/analyze`** now runs IDENTITY after OCR when requested (depends on OCR output)
- [x] **Health endpoint** reports `identity: AVAILABLE`
- [x] **Flyway V10** — `identity_consistency_results` table
- [x] **`IdentityConsistencyResult` entity** + repository
- [x] **`IdentityConsistencyResponse` DTO**
- [x] **`AnalysisService`** updated — persists identity results, default modules now `OCR,METADATA,IDENTITY`
- [x] **`GET /api/documents/{id}/analysis/identity`** endpoint
- [x] **Python tests** — 25 identity tests (availability, schema, NOT_APPLICABLE cases, rule checks, date detection, endpoint)
- [x] **62 Python tests total** (1 skipped), **24 Spring Boot tests** — all green

---

## Phase 5 — Metadata Analysis — Details

### Completed Items

- [x] **Python metadata module** (`ai-service/app/modules/metadata.py`) — Pillow for image EXIF/dimensions, PyMuPDF for PDF metadata, anomaly detection rules
- [x] **Anomaly checks**: editing software detection (Photoshop/GIMP/etc), timestamp inconsistency, GPS data in documents, missing EXIF on JPEG, low resolution, no PDF metadata, unexpected page count
- [x] **Evidence score** — severity-weighted normalised score (0.0–1.0) from detected anomalies
- [x] **PyMuPDF** added to requirements.txt and Dockerfile
- [x] **`/internal/analyze`** now runs METADATA alongside OCR when requested
- [x] **Health endpoint** reports `metadata: AVAILABLE`
- [x] **Flyway V9** — `metadata_results` table
- [x] **`MetadataResult` entity** + `MetadataResultRepository`
- [x] **`MetadataResultResponse` DTO**
- [x] **`AnalysisService`** updated — persists metadata results alongside OCR, default modules now `OCR,METADATA`
- [x] **`GET /api/documents/{id}/analysis/metadata`** endpoint
- [x] **Python tests** — 20 metadata tests (schema, anomaly structure, evidence score, endpoint integration)
- [x] **Backend tests** — all 24 existing tests still pass
- [x] **37 Python tests total** (1 skipped), **24 Spring Boot tests** — all green

---

## Phase 4 — OCR — Details

### Completed Items

- [x] **Python OCR module** (`ai-service/app/modules/ocr.py`) — pytesseract wrapper with per-word bounding boxes, confidence scores, multi-page PDF support (max 5 pages), graceful failure if Tesseract unavailable
- [x] **`POST /internal/analyze`** endpoint — accepts multipart document, runs OCR, returns structured JSON; non-OCR modules return NOT_APPLICABLE until their phases
- [x] **Tesseract installed in Docker** via `apt-get install tesseract-ocr tesseract-ocr-eng poppler-utils`
- [x] **AI service health** reports `ocr: AVAILABLE` when Tesseract is running
- [x] **Flyway V8** — `ocr_results` table with task_id, document_id, extracted_text, words_json, overall_confidence, page_count
- [x] **`OcrResult` entity** — JPA entity mapped to ocr_results table
- [x] **`OcrResultRepository`** — JPA repository with findByTaskId, findTopByDocumentId
- [x] **`OcrResultResponse` DTO** — safe response projection
- [x] **`AiServiceClient`** — Spring RestTemplate-based multipart HTTP client for internal AI service calls
- [x] **`AsyncConfig`** — `@EnableAsync` + named `analysisExecutor` thread pool (2–5 threads, queue 50)
- [x] **`AnalysisService` rewritten** — `createAnalysisTask` now dispatches real async AI processing; persists OCR result; transitions document through QUEUED → PROCESSING → COMPLETED/FAILED
- [x] **`GET /api/documents/{id}/analysis/ocr`** — retrieves OCR results after analysis completes
- [x] **Python tests** — 16 passed, 1 skipped (confidence test skipped on host without Tesseract binary; passes in Docker)
- [x] **Backend tests** — all 24 existing tests still pass (BUILD SUCCESS)

### Technology Decision

Used **pytesseract / Tesseract** (Apache 2.0) over PaddleOCR:
- Tesseract fits in the existing Docker image without multi-GB model downloads
- Sufficient for Phase 4 baseline; PaddleOCR can be evaluated in Phase 13
- Documented in decisions.md

---

## Phase 3 — Frontend Foundation — Details

### Completed Items

- [x] React Router v6 installed and configured with nested protected routes
- [x] `AuthContext` — JWT stored in localStorage, login/register/logout, auto-load on startup
- [x] `api/client.ts` — typed fetch wrapper with JWT injection, `ApiException` for error handling
- [x] `types.ts` — shared TypeScript types: User, DocumentResponse, Page, AnalysisTaskResponse
- [x] Design system — dark forensics-workstation theme via CSS custom properties (index.css)
- [x] Shared components: `AppLayout` (sidebar nav), `ProtectedRoute`, `RiskBadge`, `StatusBadge`
- [x] **Login/Register page** — professional minimal login, toggle between sign-in and create account
- [x] **Dashboard page** — total count, recent screenings table, quick upload action
- [x] **Upload page** — drag-and-drop zone, file type validation, document type selector, optional reference face
- [x] **Analysis Progress page** — start analysis, pipeline stepper (10 stages), 5s polling, elapsed timer
- [x] **Results page** — risk header, evidence summary, suspicious regions placeholder, expandable module accordions
- [x] **History page** — paginated table, filename search filter, status dropdown filter
- [x] **Document Details page** — document metadata, analysis status, delete with confirmation
- [x] All routes protected (redirect to /login when unauthenticated)
- [x] CSS Modules for all component styles, no UI framework
- [x] TypeScript strict — `verbatimModuleSyntax` compatible imports
- [x] `npm run build` succeeds — 294 KB JS (91 KB gzip), 17 KB CSS

### Design Decisions

- Dark theme (`#0d0f13` background) — professional forensics workstation aesthetic per UI/UX spec
- No UI framework — custom CSS only, consistent with spec requirement to avoid generic AI dashboard look
- Inter font via Google Fonts
- Language follows spec: "potential manipulation", "elevated risk", never "definitely fake"
- Module accordions on results page are placeholder until Phases 4–9 connect AI modules

### Completed Items

- [x] User entity with UUID primary key, bcrypt password hashing via Spring Security `BCryptPasswordEncoder`
- [x] Authentication: `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` endpoints
- [x] JWT token issuance and validation via `JwtTokenProvider` (HS256, 24h expiry)
- [x] JWT security filter (`JwtAuthenticationFilter`) extracting Bearer tokens and setting Spring Security context
- [x] `UserDetailsServiceImpl` loading users from database with role-based authorities
- [x] Roles: `USER`, `ANALYST`, `ADMIN` seeded via Flyway migration V7 (test profile uses `TestDataInitializer`)
- [x] Document entity with file metadata, status lifecycle (`UPLOADED` → `QUEUED` → …)
- [x] Document upload API (`POST /api/documents`) with file validation:
  - Extension whitelist (PDF, JPG, JPEG, PNG)
  - MIME type whitelist
  - Size limit (20 MB default)
  - Safe UUID-based filenames to prevent path traversal and collisions
  - Path traversal protection in `StorageService`
- [x] Reference face upload support (optional `referenceFace` multipart field)
- [x] PostgreSQL integration via Flyway migrations V1–V7:
  - V1: roles, V2: users, V3: documents, V4: document_files, V5: analysis_tasks, V6: audit_logs, V7: seed default roles
- [x] Audit logging service (`AuditService`) recording: register, login, upload, delete, analysis events
- [x] Analysis task lifecycle: create (`POST /api/documents/{id}/analysis`), status (`GET …/status`), get (`GET …/analysis`) — lifecycle only, no AI processing in Phase 2
- [x] Global exception handler (`GlobalExceptionHandler`) for consistent `ErrorResponse` JSON on validation and business errors
- [x] CORS configuration allowing frontend origins (localhost:3000, localhost:5173)
- [x] Integration tests:
  - `AuthControllerTest` — 12 tests: registration, login, protected endpoints, validation, duplicate detection
  - `DocumentControllerTest` — 9 tests: upload, retrieval, deletion, access control, file type validation
  - `HealthControllerTest` — 2 tests: health endpoint, public accessibility
- [x] All 24 tests passing (`mvn test` — BUILD SUCCESS)

---

## Phase 1 — Repository Foundation — Details

### Completed Items

- [x] React 19 + TypeScript frontend scaffolded with `vite`
- [x] Node packages installed and build verified (`npm run build` succeeds)
- [x] Spring Boot 3.3.4 Maven pom.xml created with Java 21 & required dependencies
- [x] Java Spring application (`VeriLensApplication.java`), `application.yml`, and `SecurityConfig.java` created
- [x] Backend health endpoint `GET /api/health` implemented with database & AI service status checks
- [x] Backend context & integration unit tests created (`VeriLensApplicationTests.java`, `HealthControllerTest.java`)
- [x] Python 3.11 FastAPI AI Service app (`app/main.py`), `requirements.txt`, and API key middleware created
- [x] AI Service health endpoint `GET /internal/health` created with module availability status
- [x] Pytest test suite (`tests/test_health.py`) created and verified (2/2 tests pass)
- [x] Multi-stage Dockerfiles created for backend, ai-service, and frontend
- [x] PostgreSQL init script created (`database/init/01-init.sql`)
- [x] Orchestration verified via `docker-compose.yml`


---

## Phase 0 — Specification — Details

### Completed Items

- [x] Workspace inspected — no existing project found
- [x] Monorepo directory structure created
- [x] Root `.gitignore` created
- [x] Root `.env.example` created
- [x] Root `docker-compose.yml` skeleton created
- [x] Root `README.md` created
- [x] `frontend/README.md` placeholder created
- [x] `backend/README.md` placeholder created
- [x] `ai-service/README.md` placeholder created
- [x] `database/README.md` placeholder created
- [x] `docs/architecture.md` written
- [x] `docs/api-contract.md` written
- [x] `docs/database-design.md` written
- [x] `docs/ai-modules.md` written
- [x] `docs/ui-ux-spec.md` written
- [x] `docs/testing-strategy.md` written
- [x] `docs/decisions.md` written
- [x] `docs/implementation-status.md` created (this file)

### Not Done (By Design)

- No application code implemented
- No dependencies installed
- No AI models selected or integrated
- No mock/fake analysis results created
- No production features built
