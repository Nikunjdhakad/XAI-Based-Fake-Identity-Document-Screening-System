# VeriLens — Viva Preparation Guide

**Version:** 1.0.0  
**Date:** 2026-11-16

---

## 1. Project Summary (Elevator Pitch)

> VeriLens is an **explainable, multi-signal AI-assisted document screening and risk-assessment platform**. It accepts identity documents and certificates, runs six independent analysis modules (OCR, metadata, identity consistency, image forensics, face verification, AI manipulation screening), fuses the evidence, and produces a transparent risk score with explanations and suspicious region visualisations. It is a **screening tool**, not a legal authentication system.

---

## 2. Key Questions & Answers

### Q: What problem does this solve?

Document fraud is a growing concern in education, employment, and identity verification. Manual verification is time-consuming and inconsistent. VeriLens automates multi-signal screening to flag potentially manipulated documents for human review, saving time and catching anomalies that humans might miss.

### Q: How is this different from just running a "fake detector" model?

VeriLens does **not** rely on a single black-box AI model. Instead, it combines six independent evidence-producing modules, each using different analysis techniques. This multi-signal approach is more robust because:
- No single technique can catch all manipulation types
- Different modules cover different attack vectors (metadata, pixels, text, faces, frequency domain)
- Evidence fusion reduces false positives from any individual module
- Every result is explainable — users see *why* a risk score is high, not just *that* it's high

### Q: Why not use deep learning models for everything?

For an academic project with specific constraints:
1. **Explainability** — Heuristic techniques produce transparent, auditable results. A CNN might say "0.87 probability fake" with no explanation.
2. **No GPU requirement** — All analysis runs on CPU, making deployment simpler.
3. **No multi-GB model downloads** — The system is self-contained.
4. **Baseline-first principle** — The master specification requires starting with explainable baselines, then evaluating deep learning in Phase 13.
5. **Honest benchmarking** — Deep learning models are recommended as future improvements with proper evaluation.

### Q: What tech stack did you use and why?

| Component | Technology | Why |
|-----------|-----------|-----|
| Frontend | React + TypeScript + Vite | Modern SPA framework, type safety, fast builds |
| Backend | Java Spring Boot 3.3 | Industry-standard Java framework, demonstrates Java competency |
| AI Service | Python FastAPI | Python is the standard for CV/ML; FastAPI is async and fast |
| Database | PostgreSQL 16 | Robust relational DB; JSONB for flexible module outputs |
| Deployment | Docker Compose | Containerised multi-service orchestration |
| Auth | JWT (jjwt) | Stateless authentication for REST APIs |

### Q: How does the architecture work?

```
React (port 3000) → Spring Boot (port 8080) → Python FastAPI (port 8000)
                              ↕
                        PostgreSQL (port 5432)
```

- **React** only talks to **Spring Boot** (never to Python directly)
- **Spring Boot** is the orchestrator — handles auth, document management, result persistence, evidence fusion, report generation
- **Python** is the AI inference engine — receives document bytes, returns structured JSON evidence
- **PostgreSQL** is accessed only by Spring Boot

### Q: Walk me through the analysis pipeline.

1. User uploads a document (PDF/JPEG/PNG) via the React frontend
2. Spring Boot validates the file (extension, MIME, magic bytes, size), stores it, creates a Document record
3. User clicks "Start Analysis" — Spring Boot creates an AnalysisTask and dispatches it to an async thread
4. The async thread sends the file to Python via `POST /internal/analyze`
5. Python runs 6 modules in sequence:
   - **OCR** (Tesseract) → extracted text + confidence
   - **Metadata** (Pillow/PyMuPDF) → file properties + anomalies
   - **Identity** (rule-based) → consistency checks on extracted fields
   - **Forensics** (OpenCV) → ELA + noise + edge analysis
   - **Face** (OpenCV Haar + histograms) → similarity with reference face
   - **AI Manipulation** (FFT + JPEG Ghost + Gradient + LBP) → manipulation screening
6. Python returns all results as JSON
7. Spring Boot persists each module's results to PostgreSQL
8. Spring Boot runs **Evidence Fusion** — weighted combination of module scores
9. Spring Boot runs **Report Generation** — creates a PDF summary
10. React polls for completion, then renders the results page

### Q: How does evidence fusion work?

Weighted average with configurable weights (v1.0.0):

| Module | Weight |
|--------|--------|
| Forensics | 25% |
| AI Manipulation | 25% |
| Metadata | 15% |
| Identity | 15% |
| OCR | 10% |
| Face | 10% |

- Missing or NOT_APPLICABLE modules are excluded from the denominator
- Risk levels: LOW (0.00–0.33), MEDIUM (0.34–0.66), HIGH (0.67–1.00)
- Human-readable explanations generated per module when anomalies detected

### Q: What are the evaluation results?

| Module | F1 Score | Key Observation |
|--------|----------|----------------|
| Forensics | 0.000 | Synthetic test limitation (ELA needs real JPEG artifacts) |
| AI Manipulation | 0.727 | 100% recall, some false positives |
| Combined | 0.762 | Fusion improves over individual modules |

- Robustness: AI manipulation achieves 100% recall under compression, blur, and resize
- IoU (localisation): 0.1676 average across 7 overlapping regions
- These are **baseline results on synthetic data**, not production accuracy claims

### Q: What are the main limitations?

1. **Synthetic evaluation only** — no real manipulated document dataset used
2. **No deep learning models** — heuristic baseline only; ManTraNet/CAT-Net recommended for future
3. **English-only OCR** — Tesseract configured for English
4. **Histogram-based face verification** — not as discriminative as ArcFace/FaceNet
5. **Single-instance deployment** — no horizontal scaling
6. **Forensics module scores 0 on synthetic data** — works on real documents, but the evaluation methodology cannot demonstrate this

### Q: How did you handle security?

1. **Authentication**: JWT with bcrypt password hashing
2. **File validation**: Extension whitelist + MIME check + magic bytes verification
3. **Path traversal protection**: UUID-based filenames, path normalisation, boundary checks
4. **Rate limiting**: Per-IP filter (60 req/min general, 10 req/min auth)
5. **Security headers**: CSP, HSTS, X-Frame-Options, X-Content-Type-Options, Referrer-Policy
6. **Error handling**: Global exception handler — no stack traces leaked to clients
7. **API isolation**: Python service only accepts internal API key, not exposed to frontend
8. **Audit trail**: All significant actions logged with user ID, IP, and timestamp

### Q: What would you do differently if starting over?

1. Start with a real manipulated document dataset from the beginning
2. Use deep face embeddings (ArcFace) instead of histogram features
3. Add WebSocket/SSE for real-time progress instead of polling
4. Consider adding a trained CNN for image manipulation detection alongside heuristics
5. Implement more comprehensive frontend unit tests

---

## 3. Architecture Diagrams to Draw on Board

### System Architecture
```
[Browser/React] --REST/JWT--> [Spring Boot] --REST/API Key--> [Python FastAPI]
                                    |
                              [PostgreSQL]
```

### Analysis Pipeline
```
Document → OCR → Metadata → Identity → Forensics → Face → AI Manip → Fusion → Risk Score → Report
```

### Evidence Fusion Formula
```
score = Σ(module_weight × module_score) / Σ(active_module_weights)
```

---

## 4. Code to Be Ready to Explain

| File | Why |
|------|-----|
| `SecurityConfig.java` | JWT filter chain, CORS, endpoint security |
| `AnalysisService.java` | Async pipeline orchestration |
| `EvidenceFusionService.java` | Weighted scoring logic |
| `ai_manipulation.py` | FFT spectral analysis, JPEG ghost detection |
| `forensics.py` | ELA implementation |
| `Results.tsx` | Risk display, suspicious region overlay |
| `run_evaluation.py` | Evaluation framework |

---

## 5. Talking Points for Academic Positioning

1. "This is an **evidence-first screening system**, not a fake detector"
2. "We **acknowledge limitations openly** — no 100% detection claims"
3. "Multi-signal fusion is more robust than any single technique"
4. "Every result is **explainable** — the user sees why, not just what"
5. "The evaluation framework is **reproducible** and can be re-run with real data"
6. "Security was not an afterthought — Phase 14 hardened the entire stack"
7. "The architecture demonstrates **real full-stack engineering**: React + Spring Boot + Python + PostgreSQL + Docker"

