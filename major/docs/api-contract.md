# VeriLens — API Contract

**Version:** 0.1.0  
**Phase:** 9 — AI Manipulation Screening  
**Last updated:** 2026-10-25

---

## 1. Overview

This document defines the API contracts between the three communicating services:

1. **React → Spring Boot** (Public API)
2. **Spring Boot → Python** (Internal API)

All endpoints use JSON request/response bodies unless otherwise noted.

---

## 2. Public API — React ↔ Spring Boot

Base URL: `/api`

### 2.1 Authentication

> **Status (Phase 2): ✅ Implemented** — All three auth endpoints are live and tested.

#### POST /api/auth/register

Create a new user account.

**Request:**
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "fullName": "string"
}
```

**Response (201):**
```json
{
  "id": "uuid",
  "username": "string",
  "email": "string",
  "fullName": "string",
  "role": "USER",
  "createdAt": "ISO-8601"
}
```

**Errors:** `400` validation, `409` duplicate username/email

---

#### POST /api/auth/login

Authenticate and receive a JWT.

**Request:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Response (200):**
```json
{
  "token": "string",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": "uuid",
    "username": "string",
    "email": "string",
    "fullName": "string",
    "role": "string"
  }
}
```

**Errors:** `401` invalid credentials

---

#### GET /api/auth/me

Get the authenticated user's profile.

**Headers:** `Authorization: Bearer <token>`

**Response (200):**
```json
{
  "id": "uuid",
  "username": "string",
  "email": "string",
  "fullName": "string",
  "role": "string",
  "createdAt": "ISO-8601"
}
```

**Errors:** `401` unauthenticated

---

### 2.2 Documents

> **Status (Phase 2): ✅ Implemented** — Upload, list, get, and delete endpoints are live and tested.

#### POST /api/documents

Upload a document for screening.

**Headers:** `Authorization: Bearer <token>`  
**Content-Type:** `multipart/form-data`

**Fields:**
| Field           | Type   | Required | Description                |
| --------------- | ------ | -------- | -------------------------- |
| `file`          | file   | yes      | Document file (PDF, JPG, PNG) |
| `documentType`  | string | yes      | Category of document       |
| `description`   | string | no       | Optional description       |
| `referenceFace` | file   | no       | Reference face image       |

**Response (201):**
```json
{
  "id": "uuid",
  "filename": "string",
  "documentType": "string",
  "status": "UPLOADED",
  "uploadedAt": "ISO-8601",
  "uploadedBy": "uuid"
}
```

**Errors:** `400` invalid file type/size, `401` unauthenticated

---

#### GET /api/documents

List all documents for the authenticated user.

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**
| Param    | Type   | Description              |
| -------- | ------ | ------------------------ |
| `page`   | int    | Page number (0-indexed)  |
| `size`   | int    | Page size (default 20)   |
| `sort`   | string | Sort field               |
| `status` | string | Filter by status         |
| `risk`   | string | Filter by risk level     |

**Response (200):**
```json
{
  "content": [
    {
      "id": "uuid",
      "filename": "string",
      "documentType": "string",
      "status": "string",
      "riskLevel": "string | null",
      "uploadedAt": "ISO-8601"
    }
  ],
  "totalElements": 0,
  "totalPages": 0,
  "page": 0,
  "size": 20
}
```

---

#### GET /api/documents/{id}

Get document details.

**Response (200):**
```json
{
  "id": "uuid",
  "filename": "string",
  "documentType": "string",
  "description": "string | null",
  "status": "string",
  "fileSize": 0,
  "mimeType": "string",
  "uploadedAt": "ISO-8601",
  "uploadedBy": "uuid"
}
```

**Errors:** `404` not found, `403` not owner

---

#### DELETE /api/documents/{id}

Delete a document and its analysis results.

**Response:** `204 No Content`

**Errors:** `404` not found, `403` not owner

---

### 2.3 Analysis

> **Status (Phase 2): ⚡ Lifecycle-only** — Create, status, and get endpoints exist but only manage the analysis task lifecycle (QUEUED status). No AI processing is triggered in Phase 2. Actual analysis will be connected in Phases 4–9.

#### POST /api/documents/{id}/analysis

Start analysis on an uploaded document.

**Request (optional):**
```json
{
  "modules": ["OCR", "FORENSICS", "METADATA", "IDENTITY", "FACE", "AI_MANIPULATION"]
}
```

If `modules` is omitted, all applicable modules run.

**Response (202):**
```json
{
  "jobId": "uuid",
  "documentId": "uuid",
  "status": "QUEUED",
  "requestedAt": "ISO-8601"
}
```

---

#### GET /api/documents/{id}/analysis/status

Check analysis progress.

**Response (200):**
```json
{
  "jobId": "uuid",
  "documentId": "uuid",
  "status": "QUEUED | PROCESSING | COMPLETED | FAILED | PARTIALLY_COMPLETED",
  "progress": {
    "ocr": "PENDING | RUNNING | COMPLETED | FAILED | NOT_APPLICABLE",
    "forensics": "PENDING | RUNNING | COMPLETED | FAILED | NOT_APPLICABLE",
    "metadata": "PENDING | RUNNING | COMPLETED | FAILED | NOT_APPLICABLE",
    "identity": "PENDING | RUNNING | COMPLETED | FAILED | NOT_APPLICABLE",
    "face": "PENDING | RUNNING | COMPLETED | FAILED | NOT_APPLICABLE",
    "aiManipulation": "PENDING | RUNNING | COMPLETED | FAILED | NOT_APPLICABLE"
  },
  "startedAt": "ISO-8601 | null",
  "completedAt": "ISO-8601 | null"
}
```

---

#### GET /api/documents/{id}/analysis

Get full analysis results.

**Response (200):**
```json
{
  "jobId": "uuid",
  "documentId": "uuid",
  "status": "COMPLETED",
  "riskAssessment": {
    "score": 0.74,
    "level": "HIGH",
    "explanation": ["string"],
    "moduleContributions": {
      "ocr": 0.1,
      "forensics": 0.85,
      "metadata": 0.45,
      "identity": 0.15,
      "face": null,
      "aiManipulation": 0.78
    },
    "version": "string"
  },
  "modules": {
    "ocr": {},
    "forensics": {},
    "metadata": {},
    "identity": {},
    "face": {},
    "aiManipulation": {}
  },
  "suspiciousRegions": [],
  "completedAt": "ISO-8601",
  "processingTimeMs": 0
}
```

The `modules` sub-objects are defined per-module in [ai-modules.md](ai-modules.md).

---

#### Per-Module Result Endpoints

After analysis completes, individual module results can be retrieved directly:

##### GET /api/documents/{id}/analysis/ocr
Returns the latest `OcrResultResponse` (extractedText, fields, confidence, status).

##### GET /api/documents/{id}/analysis/metadata
Returns the latest `MetadataResultResponse` (file properties, anomalies, evidence score, status).

##### GET /api/documents/{id}/analysis/identity
Returns the latest `IdentityConsistencyResponse` (consistency checks, consistent boolean, status).

##### GET /api/documents/{id}/analysis/forensics
Returns the latest `ForensicResultResponse` (risk score, ELA mean error, noise/edge stats, findings, suspicious regions, status).

##### GET /api/documents/{id}/analysis/face
Returns the latest `FaceVerificationResponse` (similarity, threshold, status: POSSIBLE_MATCH | POSSIBLE_MISMATCH | NOT_APPLICABLE | FAILED, face detection flags).

##### GET /api/documents/{id}/analysis/ai-manipulation
Returns the latest `AiManipulationResponse` (Phase 9):
```json
{
  "id": "uuid",
  "taskId": "uuid",
  "documentId": "uuid",
  "status": "SUCCESS | FAILED | NOT_APPLICABLE",
  "riskScore": 0.4520,
  "manipulationType": "NONE_DETECTED | SPLICING | INPAINTING | GENERATION | UNKNOWN",
  "confidence": 0.6500,
  "findingsJson": "[...]",
  "suspiciousRegionsJson": "[...]",
  "modelInfoJson": "{...}",
  "errorMessage": null,
  "processingTimeMs": 48,
  "createdAt": "ISO-8601"
}
```

---

### 2.4 Reports

#### GET /api/documents/{id}/report

Get the screening report metadata.

**Response (200):**
```json
{
  "reportId": "uuid",
  "documentId": "uuid",
  "generatedAt": "ISO-8601",
  "riskLevel": "string",
  "riskScore": 0.0
}
```

---

#### GET /api/documents/{id}/report/download

Download the screening report as PDF.

**Response:** `200 application/pdf`

---

### 2.5 Health

> **Status (Phase 2): ✅ Implemented** — Health endpoint is live and tested.

#### GET /api/health

Backend health check.

**Response (200):**
```json
{
  "status": "UP",
  "service": "verilens-backend",
  "timestamp": "ISO-8601",
  "database": "UP",
  "aiService": "UP | DOWN | UNKNOWN"
}
```

---

## 3. Internal API — Spring Boot ↔ Python

Base URL: `http://ai-service:8000/internal`

Authentication: API key in `X-API-Key` header.

### 3.1 Health

#### GET /internal/health

**Response (200):**
```json
{
  "status": "UP",
  "service": "verilens-ai-service",
  "timestamp": "ISO-8601",
  "modules": {
    "ocr": "AVAILABLE | UNAVAILABLE",
    "forensics": "AVAILABLE | UNAVAILABLE",
    "metadata": "AVAILABLE | UNAVAILABLE",
    "face": "AVAILABLE | UNAVAILABLE",
    "aiManipulation": "AVAILABLE | UNAVAILABLE"
  }
}
```

---

### 3.2 Analyze

#### POST /internal/analyze

Run analysis on a document.

**Content-Type:** `multipart/form-data`

**Fields:**
| Field            | Type   | Required | Description                     |
| ---------------- | ------ | -------- | ------------------------------- |
| `file`           | file   | yes      | Document image or PDF           |
| `documentId`     | string | yes      | UUID for tracking               |
| `modules`        | string | no       | Comma-separated module list     |
| `referenceFace`  | file   | no       | Reference face image for face verification |

**Response (200):**
```json
{
  "documentId": "uuid",
  "status": "COMPLETED | PARTIALLY_COMPLETED | FAILED",
  "modules": {
    "ocr": {
      "status": "SUCCESS | FAILED | NOT_APPLICABLE",
      "extractedText": "string",
      "fields": [],
      "confidence": 0.0,
      "processingTimeMs": 0
    },
    "forensics": {
      "status": "SUCCESS | FAILED | NOT_APPLICABLE",
      "riskScore": 0.0,
      "findings": [],
      "suspiciousRegions": [],
      "processingTimeMs": 0
    },
    "metadata": {
      "status": "SUCCESS | FAILED | NOT_APPLICABLE",
      "available": true,
      "properties": {},
      "anomalies": [],
      "evidenceScore": 0.0,
      "processingTimeMs": 0
    },
    "identityConsistency": {
      "status": "SUCCESS | FAILED | NOT_APPLICABLE",
      "consistent": true,
      "checks": [],
      "processingTimeMs": 0
    },
    "faceVerification": {
      "status": "MATCH | MISMATCH | NOT_APPLICABLE | FAILED",
      "similarity": 0.0,
      "processingTimeMs": 0
    },
    "aiManipulation": {
      "status": "SUCCESS | FAILED | NOT_APPLICABLE",
      "riskScore": 0.0,
      "manipulationType": "UNKNOWN",
      "confidence": 0.0,
      "suspiciousRegions": [],
      "processingTimeMs": 0
    }
  },
  "processingTimeMs": 0
}
```

---

## 4. Common Schemas

### Error Response

All error responses follow:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Descriptive error message",
  "timestamp": "ISO-8601",
  "path": "/api/..."
}
```

### Suspicious Region

```json
{
  "x": 0,
  "y": 0,
  "width": 0,
  "height": 0,
  "source": "OCR | FORENSICS | AI_MANIPULATION",
  "confidence": 0.0,
  "description": "string"
}
```

### Document Status Values

`UPLOADED` → `QUEUED` → `PROCESSING` → `COMPLETED` | `FAILED` | `PARTIALLY_COMPLETED`

### Risk Levels

`LOW` | `MEDIUM` | `HIGH`

---

## 5. Versioning

API contracts are versioned alongside the project phases. Breaking changes must be documented in [decisions.md](decisions.md) before implementation.

Current contract version: **0.1.0-draft**

This contract will be refined as each module is implemented (Phases 4–9).
