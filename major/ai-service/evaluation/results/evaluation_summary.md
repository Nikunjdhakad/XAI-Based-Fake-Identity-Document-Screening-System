# VeriLens — Evaluation Report (Phase 13)

**Generated:** 2026-11-04T07:00:40  
**Version:** 1.0.0

---

## 1. Test Suite Composition

| Category | Count |
| -------- | ----- |
| Genuine  | 6 |
| Manipulated | 8 |
| Robustness (degraded) | 16 |
| **Total** | **30** |

Manipulation categories: copy_paste, text_edit, resave, inpainting, genuine, generation

> [!NOTE]
> This evaluation uses **synthetic test images** with programmatically applied manipulations.
> Results represent benchmark performance, not real-world forensic accuracy.

---

## 2. Classification Metrics — Base Test Suite

| Module | Accuracy | Precision | Recall | F1 | ROC-AUC |
| ------ | -------- | --------- | ------ | -- | ------- |
| forensics | 0.429 | 0.000 | 0.000 | 0.000 | 0.000 |
| ai_manipulation | 0.571 | 0.571 | 1.000 | 0.727 | 0.521 |
| combined | 0.643 | 0.615 | 1.000 | 0.762 | 0.521 |

**Thresholds used:**
- Forensics: risk score > 0.15
- AI Manipulation: risk score > 0.2
- Combined: average score > 0.15

---

## 3. Robustness Metrics — Degraded Images

Tests module resilience to: JPEG compression (Q=30, Q=50), Gaussian blur (r=2.0), resize (50%).

| Module | Accuracy | Recall | F1 |
| ------ | -------- | ------ | -- |
| forensics | 0.000 | 0.000 | 0.000 |
| ai_manipulation | 1.000 | 1.000 | 1.000 |
| combined | 1.000 | 1.000 | 1.000 |

---

## 4. Localization — IoU Evaluation

- Overlapping regions found: **7**
- Average IoU: **0.1676**

> [!TIP]
> IoU measures how well detected suspicious regions overlap with known manipulation regions.
> Higher values indicate better spatial localization.

---

## 5. Limitations

- Evaluation uses synthetic test images, not real manipulated documents
- Test suite is small (14 base + 16 robustness) — results are indicative, not definitive
- Genuine/manipulated distribution may not match real-world prevalence
- IoU evaluation is limited by synthetic ground-truth bounding boxes
- Deep learning models (ArcFace, manipulation classifiers) not yet integrated
- OCR, metadata, identity, and face modules are not evaluated for classification accuracy here (they contribute evidence, not binary predictions)

---

## 6. Recommendations

- Collect or license a real manipulated-document dataset for Phase 14+ validation
- Tune thresholds based on precision/recall tradeoff requirements
- Consider deep learning baselines (ManTraNet, CAT-Net) for comparison in future work
- Evaluate fusion weights with larger, more diverse test sets
- Add cross-format evaluation (PDF vs scanned image vs mobile photo)

---

> [!IMPORTANT]
> VeriLens is an **evidence-first screening system**. It does not claim 100% detection accuracy.
> These evaluation results should be interpreted as baseline benchmarks for the current
> heuristic-based approach. Deep learning models and real-world datasets are needed for
> production-grade forensic accuracy assessment.
