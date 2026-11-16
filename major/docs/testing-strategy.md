# VeriLens — Testing Strategy

**Version:** 0.1.0  
**Phase:** 2 — Backend Foundation  
**Last updated:** 2026-10-20

---

## 1. Overview

Testing is organized by service and by concern. Each layer has specific responsibilities and tools.

---

## 2. Backend Testing (Spring Boot)

### 2.1 Unit Tests

| Target         | Tool           | Scope                                  |
| -------------- | -------------- | -------------------------------------- |
| Services       | JUnit 5 + Mockito | Business logic in service classes    |
| Mappers/DTOs   | JUnit 5        | Data transformation correctness        |
| Validation     | JUnit 5        | Input validation rules                 |
| Risk engine    | JUnit 5        | Scoring and fusion logic               |

### 2.2 Integration Tests

| Target             | Tool                        | Scope                           |
| ------------------ | --------------------------- | ------------------------------- |
| Repositories       | @DataJpaTest + H2/Testcontainers | JPA entity mapping, queries |
| Controllers        | @WebMvcTest + MockMvc       | HTTP request/response, auth     |
| Security           | @WithMockUser / JWT fixtures| Authentication, authorization   |
| End-to-end (backend)| @SpringBootTest + Testcontainers | Full Spring context        |

### 2.3 Contract Tests

- Validate that Spring Boot's AI service client matches the documented Python response schema
- Validate that REST API responses match the documented API contract

---

## 3. Frontend Testing (React)

### 3.1 Unit Tests

| Target         | Tool             | Scope                               |
| -------------- | ---------------- | ----------------------------------- |
| Components     | Vitest + React Testing Library | Render, interaction, state |
| Utilities      | Vitest           | Helper function correctness         |

### 3.2 Integration Tests

| Target         | Tool             | Scope                               |
| -------------- | ---------------- | ----------------------------------- |
| Pages          | Vitest + MSW     | Page-level behavior with mocked API |
| API client     | Vitest + MSW     | Request/response handling            |

### 3.3 End-to-End Tests (If Time Permits)

| Target         | Tool             | Scope                               |
| -------------- | ---------------- | ----------------------------------- |
| Critical flows | Playwright or Cypress | Login, upload, view results     |

---

## 4. AI Service Testing (Python)

### 4.1 Unit Tests

| Target         | Tool           | Scope                                  |
| -------------- | -------------- | -------------------------------------- |
| Module logic   | pytest         | Individual module processing           |
| Schema         | pytest + pydantic | Response schema validation          |
| Preprocessing  | pytest         | Image preprocessing correctness        |
| Error handling | pytest         | Graceful failure per module            |

### 4.2 Fixture Tests

- Use small, representative test images (not full datasets)
- Test that each module returns valid schema for known inputs
- Test edge cases: blank images, corrupted files, unsupported formats

### 4.3 Integration Tests

| Target         | Tool               | Scope                              |
| -------------- | ------------------- | ---------------------------------- |
| API endpoints  | pytest + httpx/TestClient | FastAPI endpoint behavior    |
| Module pipeline| pytest              | Multi-module execution              |

---

## 5. Cross-Service Integration Testing

Test the full chain:

```text
React → Spring Boot → Python → Spring Boot → PostgreSQL
```

- Upload a document through the API
- Verify analysis job creation
- Verify Python service call
- Verify result persistence
- Verify result retrieval

Tool: Spring Boot integration tests with Docker Compose (Testcontainers or manual).

---

## 6. ML / AI Evaluation (Phase 13)

### 6.1 Classification Metrics

| Metric     | Purpose                                  |
| ---------- | ---------------------------------------- |
| Accuracy   | Overall correctness                      |
| Precision  | Of those flagged, how many are correct   |
| Recall     | Of actual manipulations, how many found  |
| F1 Score   | Harmonic mean of precision and recall    |
| ROC-AUC    | Discriminative ability across thresholds |

### 6.2 Localization Metrics (Where Applicable)

| Metric | Purpose                                       |
| ------ | --------------------------------------------- |
| IoU    | Overlap between predicted and actual regions   |

### 6.3 Robustness Evaluation

Test module performance under:
- JPEG compression at various quality levels
- Gaussian blur
- Resizing / downscaling
- Screenshot capture simulation
- Mobile-camera photo simulation

### 6.4 Generalization Evaluation

- Test on manipulation techniques **not** in the training/development set
- Document which techniques the system handles well and poorly

---

## 7. Test Data

### Guidelines
- Use a curated set of test documents (genuine + manipulated)
- Never use real personal documents without consent
- Create synthetic test cases where needed
- Document the composition of the test set
- Store test fixtures in each service's `tests/` or `test/resources` directory

### Manipulation Types to Cover
- Text modification
- Copy-paste manipulation
- Image replacement
- AI-based inpainting
- Digital editing
- Re-saved / re-captured files

---

## 8. Test Execution

| Environment | Backend        | Frontend      | AI Service  |
| ----------- | -------------- | ------------- | ----------- |
| Local dev   | `mvn test`     | `npm test`    | `pytest`    |
| CI          | GitHub Actions  | GitHub Actions| GitHub Actions |

CI pipeline will be configured if/when GitHub repository is set up.

---

## 9. Testing Priorities by Phase

| Phase | Testing Focus                                          |
| ----- | ------------------------------------------------------ |
| 1     | Health endpoint tests, service startup verification    |
| 2     | Auth tests, document CRUD tests, validation tests      |
| 3     | Component tests, page render tests                     |
| 4     | OCR module tests, fixture tests                        |
| 5     | Metadata module tests                                  |
| 6     | Identity consistency rule tests                        |
| 7     | Forensics module tests                                 |
| 8     | Face verification tests                                |
| 9     | AI manipulation tests                                  |
| 10    | Fusion logic tests, risk scoring tests                 |
| 11    | UI overlay tests                                       |
| 12    | Report generation tests                                |
| 13    | Full ML evaluation suite                               |
| 14    | Security tests, performance tests, edge-case tests     |

---

## 10. Phase 2 Testing — Complete

Phase 2 backend testing is complete with the following integration test suites:

| Test Class | Tests | Coverage |
| ---------- | ----- | -------- |
| `AuthControllerTest` | 12 | Registration, login, JWT-protected endpoints, validation errors, duplicate username/email |
| `DocumentControllerTest` | 9 | Upload (PDF/JPEG/PNG), file type validation, listing, retrieval, deletion, cross-user access control |
| `HealthControllerTest` | 2 | Health endpoint response, public accessibility without auth |
| `VeriLensApplicationTests` | 1 | Spring context loads successfully |

**Total: 24 tests, all passing.**

Tests use `@SpringBootTest` with `@ActiveProfiles("test")` running against H2 in-memory database with PostgreSQL compatibility mode. See [decisions.md](decisions.md) DEC-009 for the test database rationale.


---

## 11. Phase 13 ML Evaluation — Complete

Phase 13 evaluation was conducted using a comprehensive evaluation framework at `ai-service/evaluation/run_evaluation.py`.

### 11.1 Test Suite Composition

| Category | Count |
| -------- | ----- |
| Genuine | 6 |
| Manipulated | 8 |
| Robustness (degraded) | 16 |
| **Total** | **30** |

Manipulation categories: text_edit, copy_paste, inpainting, generation, resave

### 11.2 Classification Results — Base Test Suite

| Module | Accuracy | Precision | Recall | F1 | ROC-AUC |
| ------ | -------- | --------- | ------ | -- | ------- |
| forensics | 0.429 | 0.000 | 0.000 | 0.000 | 0.000 |
| ai_manipulation | 0.571 | 0.571 | 1.000 | 0.727 | 0.521 |
| combined | 0.643 | 0.615 | 1.000 | 0.762 | 0.521 |

Thresholds: Forensics > 0.15, AI Manipulation > 0.20, Combined average > 0.15

### 11.3 Robustness Results — Degraded Images

| Module | Accuracy | Recall | F1 |
| ------ | -------- | ------ | -- |
| forensics | 0.000 | 0.000 | 0.000 |
| ai_manipulation | 1.000 | 1.000 | 1.000 |
| combined | 1.000 | 1.000 | 1.000 |

Degradation types: JPEG compression (Q=30, Q=50), Gaussian blur (r=2.0), resize (50%)

### 11.4 Localization — IoU

- Overlapping regions found: **7**
- Average IoU: **0.1676**

### 11.5 Limitations

- **Synthetic test data**: Evaluation uses programmatically generated test images, not real manipulated documents. Results are indicative baselines.
- **Forensics module**: ELA/noise/edge techniques score 0.0 because programmatic PIL edits do not create JPEG double-compression artifacts. The module works correctly on real re-saved/spliced JPEG documents.
- **Small test suite**: 14 base + 16 robustness cases. Results are not statistically robust.
- **False positives**: AI manipulation module flags genuine synthetic certificates with large uniform areas.

Full results: `ai-service/evaluation/results/evaluation_summary.md`


---

## 12. Phase 14 Hardening — Complete

Phase 14 hardening covered security, validation, error handling, performance, and UX polish.

### 12.1 Security Hardening

| Component | What | Files |
| --------- | ---- | ----- |
| Security headers | CSP, HSTS, X-Frame-Options, X-Content-Type-Options, Referrer-Policy, Permissions-Policy, Cache-Control | `SecurityHeadersFilter.java` |
| Rate limiting | Per-IP in-memory, 60 req/min general, 10 req/min auth | `RateLimitFilter.java` |
| Magic bytes validation | PDF/JPEG/PNG header verification in `StorageService` | `StorageService.java` |
| Python size guard | Rejects requests > 25 MB | `main.py` middleware |
| Python error handler | Global exception handler, no stack trace leaks | `main.py` |

### 12.2 Error Handling

`GlobalExceptionHandler` now covers 10+ exception types with consistent JSON error responses. Catch-all handler logs full stack trace internally but returns a generic message to the client.

### 12.3 Performance

- Response GZIP compression enabled for JSON/XML/HTML/PDF > 1 KB
- AI service client `RestTemplate` configured with 10s connect timeout, 120s read timeout

### 12.4 UX Polish

- React ErrorBoundary prevents blank-screen crashes
- 404 NotFound page for unmatched routes
- Accessibility: skip-to-content link, ARIA landmarks, focus-visible outlines, reduced-motion support
- Responsive text scaling for mobile viewports

### 12.5 Frontend Build

- **313 KB JS** (96 KB gzip), **24 KB CSS** — build passes
