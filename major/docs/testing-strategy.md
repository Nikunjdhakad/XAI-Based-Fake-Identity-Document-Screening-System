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
