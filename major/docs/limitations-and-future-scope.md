# VeriLens — Limitations & Future Scope

**Version:** 1.0.0  
**Date:** 2026-11-16

---

## Part 1: Known Limitations

### 1.1 OCR Limitations

| Limitation | Impact | Mitigation |
|-----------|--------|-----------|
| English-only OCR | Cannot extract text from non-English documents | Tesseract supports language packs; additional languages can be added |
| Low-quality image sensitivity | OCR accuracy degrades on blurry, rotated, or low-resolution images | Confidence scores are preserved and surfaced to the user |
| No layout understanding | OCR extracts text sequentially, not by semantic document regions | Identity consistency module applies rule-based field detection |
| Handwritten text | Tesseract has limited handwriting recognition | Not applicable for most typed identity documents |

### 1.2 Image Forensics Limitations

| Limitation | Impact | Mitigation |
|-----------|--------|-----------|
| ELA requires JPEG compression history | ELA is ineffective on PNG-only or programmatically generated images | Multiple techniques (noise, edge) supplement ELA |
| High compression may mask artifacts | Heavily compressed images reduce ELA sensitivity | Robustness evaluation documents this behaviour |
| Copy-move detection not pixel-precise | Block-based analysis produces coarse bounding boxes | IoU = 0.1676; acceptable for screening, not forensic evidence |
| No deep learning forensics | Missing ManTraNet, CAT-Net, or similar trained detectors | Phase 13 recommends future integration |

### 1.3 AI Manipulation Screening Limitations

| Limitation | Impact | Mitigation |
|-----------|--------|-----------|
| False positives on uniform regions | Certificates with large plain areas trigger spectral/gradient techniques | Threshold tuning can reduce false positives |
| No trained manipulation classifier | Cannot detect novel manipulation techniques unseen in design | Evidence fusion reduces single-module dependence |
| High-quality diffusion models may evade detection | Post-processing or noise injection can mask frequency signatures | Documented as inherent limitation of heuristic approaches |
| Compression-induced false positives | Aggressive JPEG/screenshot re-encoding attenuates high frequencies naturally | Combined scoring mitigates |

### 1.4 Face Verification Limitations

| Limitation | Impact | Mitigation |
|-----------|--------|-----------|
| Haar cascade face detection | Less accurate than deep learning detectors (MTCNN, RetinaFace) | Returns NOT_APPLICABLE when no face is found |
| Histogram-based features (not deep embeddings) | Lower discriminative power than ArcFace/FaceNet | Produces POSSIBLE_MATCH/POSSIBLE_MISMATCH (never absolute claims) |
| Single face per image | Cannot handle documents with multiple face regions | Selects largest detected face |
| Lighting/pose sensitivity | Accuracy degrades with significant lighting/pose variation | Similarity threshold is configurable (default 0.70) |

### 1.5 System-Level Limitations

| Limitation | Impact |
|-----------|--------|
| Single-instance deployment | Cannot scale horizontally; limited to one server |
| In-memory rate limiting | Resets on restart; not distributed across instances |
| No real-time notification | Frontend polls for analysis status (no WebSocket/SSE) |
| No multi-document batch processing | One document analysed at a time per request |
| No document template matching | Cannot compare against known genuine templates |
| No blockchain/tamper-proof audit | Audit logs are in PostgreSQL, not immutable |

### 1.6 Evaluation Limitations

| Limitation | Impact |
|-----------|--------|
| Synthetic test data only | Results are indicative baselines, not production accuracy |
| Small test suite (30 cases) | Not statistically robust; confidence intervals are wide |
| No real manipulated document dataset | Cannot validate against known forensic benchmarks (CASIA, Columbia) |
| No cross-format evaluation | PDF vs. scanned image vs. mobile photo performance not compared |

---

## Part 2: Future Scope

### 2.1 Short-Term Improvements (Next Version)

| Enhancement | Description | Effort |
|------------|-------------|--------|
| **Real dataset evaluation** | Acquire/license CASIA v2.0 or Columbia dataset for production-grade metrics | Medium |
| **Threshold tuning** | Optimize per-module thresholds using ROC curve analysis on real data | Low |
| **Multi-language OCR** | Add Tesseract language packs for Hindi, Arabic, Chinese, etc. | Low |
| **WebSocket progress** | Replace polling with WebSocket for real-time analysis progress | Medium |
| **Batch upload** | Allow multiple document uploads and analysis in a single session | Medium |

### 2.2 Medium-Term Enhancements

| Enhancement | Description | Effort |
|------------|-------------|--------|
| **Deep learning forensics** | Integrate ManTraNet or CAT-Net for pixel-level manipulation detection | High |
| **Deep face embeddings** | Replace histogram features with ArcFace/FaceNet for better verification | High |
| **PaddleOCR integration** | Evaluate PaddleOCR for multi-language, higher-accuracy text extraction | Medium |
| **Document template matching** | Compare uploaded documents against known genuine templates per institution | High |
| **Admin dashboard** | User management, system statistics, configuration UI | Medium |
| **API rate limiting with Redis** | Distributed rate limiting for multi-instance deployment | Medium |

### 2.3 Long-Term Vision

| Enhancement | Description |
|------------|-------------|
| **Federated learning** | Train manipulation detectors across institutions without sharing documents |
| **Blockchain audit trail** | Immutable screening records for regulatory compliance |
| **Government database integration** | Optional verification against issuing authority APIs (with consent) |
| **Mobile SDK** | Native mobile capture with guided framing and instant preliminary screening |
| **Continuous learning** | Retrain detection models on newly discovered manipulation techniques |
| **Multi-modal analysis** | Cross-reference document text with external databases (university records, etc.) |
| **Explainable AI dashboard** | SHAP/LIME visualizations for model decision transparency |

### 2.4 Research Directions

1. **Adversarial robustness** — Evaluate system resilience against adversarial attacks specifically designed to evade document forgery detection
2. **Cross-lingual identity consistency** — Rules that work across multiple languages and naming conventions
3. **Generative AI detection** — Track emerging techniques for detecting outputs from GPT-4o image generation, Flux, Stable Diffusion 3, etc.
4. **Privacy-preserving screening** — Analyze documents without storing personal information (process-and-forget architecture)
5. **Benchmark dataset creation** — Create and publish an open benchmark dataset for document manipulation detection research

