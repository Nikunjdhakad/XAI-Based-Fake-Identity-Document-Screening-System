# VeriLens — AI Module Specifications

**Version:** 0.1.0  
**Phase:** 0 — Specification  
**Last updated:** 2026-10-07

---

## 1. Overview

VeriLens uses six evidence-producing analysis modules. Each module returns structured JSON evidence — none claims to independently prove a document is genuine or fake.

All modules run within the **Python AI Service** (FastAPI) and are called by **Spring Boot** via the internal REST API.

```text
Document
   │
   ├──▶ 1. OCR
   ├──▶ 2. Image Forensics
   ├──▶ 3. Metadata Analysis
   ├──▶ 4. Identity Consistency
   ├──▶ 5. Face Verification
   └──▶ 6. AI Manipulation Screening
                   │
                   ▼
           Evidence Fusion (Spring Boot)
                   │
                   ▼
            Risk Score Engine
                   │
                   ▼
         Explainable Screening Report
```

---

## 2. Module Status

| Module                  | Phase | Status      |
| ----------------------- | ----- | ----------- |
| OCR                     | 4     | NOT STARTED |
| Image Forensics         | 7     | NOT STARTED |
| Metadata Analysis       | 5     | NOT STARTED |
| Identity Consistency    | 6     | NOT STARTED |
| Face Verification       | 8     | NOT STARTED |
| AI Manipulation Screening | 9   | NOT STARTED |
| Evidence Fusion         | 10    | NOT STARTED |
| Risk Score Engine       | 10    | NOT STARTED |

---

## 3. Module 1 — OCR

### Purpose
Convert document images and PDF pages into machine-readable text with spatial information.

### Candidate Technology
- **PaddleOCR** — multi-language, bounding box support, Apache 2.0 license
- **Tesseract** — established, widely used, Apache 2.0 license

Technology selection will be finalized in Phase 4 after evaluation.

### Input
- Document image (JPEG, PNG) or rendered PDF page

### Output Schema
```json
{
  "status": "SUCCESS | FAILED | NOT_APPLICABLE",
  "extractedText": "full extracted text",
  "fields": [
    {
      "text": "string",
      "field": "name | date | id_number | institution | ...",
      "confidence": 0.0,
      "bbox": [x1, y1, x2, y2]
    }
  ],
  "overallConfidence": 0.0,
  "processingTimeMs": 0
}
```

### Important Notes
- OCR output is **evidence**, not ground truth
- Confidence values should be preserved and surfaced
- Field extraction may require post-processing rules per document type
- Low-quality images will produce lower confidence

---

## 4. Module 2 — Image Forensics

### Purpose
Identify image-level indicators that may suggest manipulation.

### Candidate Techniques
- JPEG compression artifact analysis (double compression detection)
- Error Level Analysis (ELA)
- Noise inconsistency analysis
- Copy-move detection
- Edge and texture anomaly detection
- Model-based manipulation detection (if suitable model found)

### Technology
- **OpenCV** — image processing, analysis
- **scikit-learn** — classical ML if applicable
- **PyTorch** — if a pretrained model is used

### Input
- Document image (JPEG, PNG)

### Output Schema
```json
{
  "status": "SUCCESS | FAILED | NOT_APPLICABLE",
  "riskScore": 0.0,
  "findings": [
    {
      "type": "string",
      "severity": "LOW | MEDIUM | HIGH",
      "description": "string",
      "confidence": 0.0
    }
  ],
  "suspiciousRegions": [
    {
      "x": 0, "y": 0, "width": 0, "height": 0,
      "confidence": 0.0,
      "description": "string"
    }
  ],
  "processingTimeMs": 0
}
```

### Important Notes
- OpenCV alone does not prove forgery
- Techniques will be selected after baseline experimentation
- Start with explainable, interpretable techniques before adding deep models
- Document limitations honestly

---

## 5. Module 3 — Metadata Analysis

### Purpose
Extract and analyze file-level metadata for inconsistencies.

### Analyzed Properties
- File type and format
- Image dimensions and resolution
- Creation / modification timestamps
- Software / producer information
- PDF metadata (author, creator, producer, page count)
- EXIF data (camera model, GPS, software, dates)
- Compression characteristics

### Technology
- **Pillow** — image metadata
- **PyMuPDF** — PDF metadata
- **Python standard library** — file-level info

### Input
- Document file (JPEG, PNG, PDF)

### Output Schema
```json
{
  "status": "SUCCESS | FAILED | NOT_APPLICABLE",
  "available": true,
  "properties": {
    "fileType": "string",
    "dimensions": {"width": 0, "height": 0},
    "timestamps": {},
    "software": "string | null",
    "exif": {}
  },
  "anomalies": [
    {
      "type": "string",
      "severity": "LOW | MEDIUM | HIGH",
      "description": "string"
    }
  ],
  "evidenceScore": 0.0,
  "processingTimeMs": 0
}
```

### Important Notes
- Metadata can be missing or deliberately stripped — absence is not proof of tampering
- Metadata can also be spoofed — presence is not proof of authenticity
- Metadata is **supporting evidence**

---

## 6. Module 4 — Identity Consistency

### Purpose
Check whether extracted document fields are internally consistent.

### Example Checks
- Date relationships (issue date before expiry, birth date plausible for issue date)
- Field format validation (ID number patterns, date formats)
- Required field presence
- Name consistency across document fields
- Impossible or contradictory values

### Technology
- Primarily rule-based logic (Python)
- Depends on OCR output

### Input
- Extracted OCR fields for the document

### Output Schema
```json
{
  "status": "SUCCESS | FAILED | NOT_APPLICABLE",
  "consistent": true,
  "checks": [
    {
      "rule": "string",
      "passed": true,
      "message": "string",
      "severity": "LOW | MEDIUM | HIGH"
    }
  ],
  "processingTimeMs": 0
}
```

### Important Notes
- Does **not** connect to government databases
- Rules are document-type specific
- Some checks depend on OCR accuracy

---

## 7. Module 5 — Face Verification

### Purpose
Compare a face detected in the document against a reference face image when provided.

### Pipeline
```text
Document → Face Detection → Face Crop → Embedding Model → Similarity Comparison → Result
```

### Technology
- Face detection: to be evaluated (MTCNN, RetinaFace, or similar)
- Face embedding: to be evaluated (ArcFace, FaceNet, or similar)
- Similarity: cosine similarity

Technology selection will be finalized in Phase 8.

### Input
- Document image
- Reference face image (optional)

### Output Schema
```json
{
  "status": "MATCH | MISMATCH | NOT_APPLICABLE | FAILED",
  "similarity": 0.0,
  "threshold": 0.0,
  "documentFaceDetected": true,
  "processingTimeMs": 0
}
```

### Important Notes
- If no reference image is provided → `NOT_APPLICABLE`
- If no face is detected in the document → report detection failure
- This is a **similarity signal**, not legal identity verification
- Thresholds must be configurable and validated experimentally

---

## 8. Module 6 — AI Manipulation Screening

### Purpose
Screen for possible AI-assisted manipulation (inpainting, generation, deepfake edits).

### Technology Selection Process
1. Identify suitable open/research models
2. Verify license compatibility
3. Verify input/output requirements
4. Test relevance to document manipulation (not just natural photos)
5. Benchmark on available datasets
6. Select a defensible baseline
7. Document all limitations

**No model has been selected yet.** This will be done in Phase 9.

### Input
- Document image

### Output Schema
```json
{
  "status": "SUCCESS | FAILED | NOT_APPLICABLE",
  "riskScore": 0.0,
  "manipulationType": "UNKNOWN | INPAINTING | SPLICING | GENERATION | NONE_DETECTED",
  "confidence": 0.0,
  "suspiciousRegions": [],
  "modelInfo": {
    "name": "string",
    "version": "string"
  },
  "processingTimeMs": 0
}
```

### Important Notes
- AI detection is **not 100% reliable**
- Models may not generalize to unseen manipulation techniques
- Degraded images (compression, screenshots) reduce performance
- Never claim certainty where none exists

---

## 9. Evidence Fusion (Spring Boot)

### Purpose
Combine individual module evidence into a unified risk assessment.

### Design Principles
- Normalized module scores (0.0 to 1.0)
- Configurable, auditable weights
- Handle missing/failed modules explicitly
- Confidence-aware aggregation
- Store fusion configuration version for auditability

### Approach
Start with a **transparent, weighted baseline**. Do not hard-code arbitrary weights before experimental validation.

### Output
```json
{
  "score": 0.74,
  "level": "HIGH",
  "explanation": ["string"],
  "moduleContributions": {
    "ocr": 0.0,
    "forensics": 0.0,
    "metadata": 0.0,
    "identity": 0.0,
    "face": null,
    "aiManipulation": 0.0
  },
  "version": "1.0.0"
}
```

### Risk Level Thresholds (Initial — Subject to Validation)
| Score Range | Level  |
| ----------- | ------ |
| 0.00 – 0.33 | LOW    |
| 0.34 – 0.66 | MEDIUM |
| 0.67 – 1.00 | HIGH   |

These thresholds are **initial estimates** and will be refined through evaluation in Phase 13.

---

## 10. Module Independence

Each module must be able to fail independently without causing the entire analysis to fail.

```text
OCR             SUCCESS
Forensics       SUCCESS
Metadata        SUCCESS
Identity        SUCCESS
Face            NOT_APPLICABLE
AI Screening    FAILED
─────────────────────────
Overall         PARTIALLY_COMPLETED
```

The fusion layer understands module states and adjusts scoring accordingly.
