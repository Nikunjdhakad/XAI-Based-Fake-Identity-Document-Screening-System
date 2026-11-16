"""
VeriLens Evaluation Framework — Phase 13.

Generates synthetic test images with known ground truth (genuine vs manipulated),
runs all AI modules, computes classification metrics (accuracy, precision, recall,
F1, ROC-AUC) and robustness metrics, and produces a structured evaluation report.

Usage:
    python -m evaluation.run_evaluation [--output-dir results]

IMPORTANT:
    This framework uses synthetic test cases because real manipulated-document
    datasets are not freely available. Results represent performance on the
    synthetic benchmark, not real-world forensic accuracy. Real-world performance
    will differ and should be validated with domain-specific datasets.
"""

import io
import os
import sys
import json
import time
import math
import random
import hashlib
from pathlib import Path
from dataclasses import dataclass, field, asdict
from typing import Optional

import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

# ---------------------------------------------------------------------------
# Ensure the parent app is importable
# ---------------------------------------------------------------------------
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.modules import forensics, ai_manipulation

# Optional modules — may not be available outside Docker
try:
    from app.modules import metadata as metadata_mod
    METADATA_AVAILABLE = True
except Exception:
    METADATA_AVAILABLE = False

try:
    from app.modules import identity as identity_mod
    IDENTITY_AVAILABLE = True
except Exception:
    IDENTITY_AVAILABLE = False


# ---------------------------------------------------------------------------
# Test case definitions
# ---------------------------------------------------------------------------

@dataclass
class TestCase:
    """A single evaluation test case with ground truth."""
    name: str
    category: str               # "genuine", "text_edit", "copy_paste", "inpainting", "generation", "resave"
    is_manipulated: bool        # Ground truth label
    description: str
    image_bytes: bytes = field(repr=False)
    mime_type: str = "image/jpeg"
    manipulation_region: Optional[dict] = None  # Ground truth bbox {x, y, width, height}


@dataclass
class ModuleResult:
    """Result of running a module on a test case."""
    test_name: str
    module: str
    risk_score: float           # 0.0–1.0
    predicted_manipulated: bool
    regions_detected: int
    processing_time_ms: int
    status: str
    details: dict = field(default_factory=dict)


# ---------------------------------------------------------------------------
# Synthetic test image generators
# ---------------------------------------------------------------------------

def _create_base_document(width: int = 595, height: int = 842,
                          text: str = "CERTIFICATE OF ACHIEVEMENT") -> Image.Image:
    """Create a synthetic document image that looks like a certificate."""
    img = Image.new("RGB", (width, height), (255, 252, 245))
    draw = ImageDraw.Draw(img)

    # Border
    draw.rectangle([20, 20, width - 20, height - 20], outline=(80, 80, 80), width=2)
    draw.rectangle([30, 30, width - 30, height - 30], outline=(160, 160, 160), width=1)

    # Title
    try:
        font_title = ImageFont.truetype("arial.ttf", 28)
        font_body = ImageFont.truetype("arial.ttf", 16)
        font_small = ImageFont.truetype("arial.ttf", 12)
    except (OSError, IOError):
        font_title = ImageFont.load_default()
        font_body = font_title
        font_small = font_title

    draw.text((width // 2, 100), text, fill=(30, 30, 30), font=font_title, anchor="mm")

    # Body text
    body_lines = [
        "This is to certify that",
        "JOHN DOE",
        "has successfully completed the requirements",
        "for the program of study in",
        "Computer Science and Engineering",
        "",
        "Date of Issue: 15/06/2024",
        "Registration No: CS-2024-001234",
        "",
        "Institution: National Institute of Technology",
    ]
    y = 180
    for line in body_lines:
        font = font_body if line and line == line.upper() and len(line) < 30 else font_small
        draw.text((width // 2, y), line, fill=(40, 40, 40), font=font, anchor="mm")
        y += 30

    # Signature area
    draw.line([(150, height - 150), (300, height - 150)], fill=(80, 80, 80), width=1)
    draw.text((225, height - 135), "Signature", fill=(120, 120, 120), font=font_small, anchor="mm")

    # Stamp area (circle)
    draw.ellipse([width - 200, height - 220, width - 100, height - 120],
                 outline=(0, 0, 150), width=2)
    draw.text((width - 150, height - 170), "SEAL", fill=(0, 0, 150), font=font_small, anchor="mm")

    return img


def _add_noise(img: Image.Image, intensity: float = 10.0) -> Image.Image:
    """Add Gaussian noise to an image."""
    arr = np.array(img, dtype=np.float32)
    noise = np.random.normal(0, intensity, arr.shape)
    arr = np.clip(arr + noise, 0, 255).astype(np.uint8)
    return Image.fromarray(arr)


def _to_jpeg_bytes(img: Image.Image, quality: int = 85) -> bytes:
    """Convert PIL image to JPEG bytes."""
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=quality)
    return buf.getvalue()


def _to_png_bytes(img: Image.Image) -> bytes:
    """Convert PIL image to PNG bytes."""
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


# --- Genuine test cases ---

def gen_genuine_clean() -> TestCase:
    img = _create_base_document()
    return TestCase(
        name="genuine_clean",
        category="genuine",
        is_manipulated=False,
        description="Clean synthetic certificate, no manipulation",
        image_bytes=_to_jpeg_bytes(img, quality=95),
    )

def gen_genuine_noisy() -> TestCase:
    img = _add_noise(_create_base_document(), intensity=8)
    return TestCase(
        name="genuine_noisy",
        category="genuine",
        is_manipulated=False,
        description="Certificate with natural sensor noise",
        image_bytes=_to_jpeg_bytes(img, quality=85),
    )

def gen_genuine_compressed() -> TestCase:
    img = _create_base_document()
    return TestCase(
        name="genuine_compressed",
        category="genuine",
        is_manipulated=False,
        description="Heavily JPEG-compressed certificate (Q=40)",
        image_bytes=_to_jpeg_bytes(img, quality=40),
    )

def gen_genuine_resized() -> TestCase:
    img = _create_base_document()
    img = img.resize((298, 421), Image.Resampling.LANCZOS)
    return TestCase(
        name="genuine_resized",
        category="genuine",
        is_manipulated=False,
        description="Certificate downscaled to 50%",
        image_bytes=_to_jpeg_bytes(img, quality=80),
    )

def gen_genuine_blurred() -> TestCase:
    img = _create_base_document()
    img = img.filter(ImageFilter.GaussianBlur(radius=1.5))
    return TestCase(
        name="genuine_blurred",
        category="genuine",
        is_manipulated=False,
        description="Certificate with slight Gaussian blur (camera simulation)",
        image_bytes=_to_jpeg_bytes(img, quality=80),
    )

def gen_genuine_png() -> TestCase:
    img = _create_base_document()
    return TestCase(
        name="genuine_png",
        category="genuine",
        is_manipulated=False,
        description="Clean certificate saved as PNG",
        image_bytes=_to_png_bytes(img),
        mime_type="image/png",
    )


# --- Manipulated test cases ---

def gen_text_edit() -> TestCase:
    """Simulate text editing — different compression in the edited region."""
    img = _create_base_document()
    draw = ImageDraw.Draw(img)
    # Overwrite the name with a different name (simulating text edit)
    # First blank out the original text area
    draw.rectangle([150, 195, 445, 225], fill=(255, 252, 245))
    try:
        font = ImageFont.truetype("arial.ttf", 16)
    except (OSError, IOError):
        font = ImageFont.load_default()
    draw.text((297, 210), "JANE SMITH", fill=(40, 40, 40), font=font, anchor="mm")

    # Save at different quality to simulate edit artifacts
    buf1 = io.BytesIO()
    img.save(buf1, format="JPEG", quality=95)
    # Re-open and save at different quality (simulating re-save after edit)
    img2 = Image.open(buf1)
    return TestCase(
        name="text_edit_name",
        category="text_edit",
        is_manipulated=True,
        description="Name field overwritten with different text",
        image_bytes=_to_jpeg_bytes(img2, quality=75),
        manipulation_region={"x": 150, "y": 195, "width": 295, "height": 30},
    )

def gen_text_edit_date() -> TestCase:
    """Simulate editing the date field."""
    img = _create_base_document()
    draw = ImageDraw.Draw(img)
    draw.rectangle([200, 285, 400, 310], fill=(255, 252, 245))
    try:
        font = ImageFont.truetype("arial.ttf", 12)
    except (OSError, IOError):
        font = ImageFont.load_default()
    draw.text((297, 297), "Date of Issue: 01/01/2020", fill=(40, 40, 40), font=font, anchor="mm")
    return TestCase(
        name="text_edit_date",
        category="text_edit",
        is_manipulated=True,
        description="Date field edited to different value",
        image_bytes=_to_jpeg_bytes(img, quality=70),
        manipulation_region={"x": 200, "y": 285, "width": 200, "height": 25},
    )

def gen_copy_paste() -> TestCase:
    """Simulate copy-paste — paste a region from one image quality into another."""
    img = _create_base_document()
    # Save at high quality
    high_q = _to_jpeg_bytes(img, quality=95)
    img_hq = Image.open(io.BytesIO(high_q))

    # Save same image at low quality
    low_q = _to_jpeg_bytes(img, quality=50)
    img_lq = Image.open(io.BytesIO(low_q))

    # Paste a region from the high-quality into the low-quality
    region = img_hq.crop((100, 100, 300, 250))
    img_lq.paste(region, (100, 100))

    return TestCase(
        name="copy_paste_quality_mismatch",
        category="copy_paste",
        is_manipulated=True,
        description="Region from Q95 pasted into Q50 image — compression inconsistency",
        image_bytes=_to_jpeg_bytes(img_lq, quality=60),
        manipulation_region={"x": 100, "y": 100, "width": 200, "height": 150},
    )

def gen_copy_paste_stamp() -> TestCase:
    """Simulate stamp duplication — copy the seal to a different position."""
    img = _create_base_document()
    # Copy the stamp/seal area and paste it elsewhere
    stamp = img.crop((395, 622, 495, 722))
    img.paste(stamp, (50, 600))
    return TestCase(
        name="copy_paste_stamp",
        category="copy_paste",
        is_manipulated=True,
        description="Seal/stamp region duplicated to new location",
        image_bytes=_to_jpeg_bytes(img, quality=80),
        manipulation_region={"x": 50, "y": 600, "width": 100, "height": 100},
    )

def gen_inpainting_smooth() -> TestCase:
    """Simulate AI inpainting — smooth fill in a region."""
    img = _create_base_document()
    arr = np.array(img)
    # Create an unnaturally smooth fill in a region
    y1, y2, x1, x2 = 200, 280, 100, 400
    # Smooth the region with heavy Gaussian blur (simulating inpainting)
    region = arr[y1:y2, x1:x2].copy()
    from PIL import ImageFilter
    region_pil = Image.fromarray(region)
    region_pil = region_pil.filter(ImageFilter.GaussianBlur(radius=5))
    arr[y1:y2, x1:x2] = np.array(region_pil)
    img = Image.fromarray(arr)
    return TestCase(
        name="inpainting_smooth",
        category="inpainting",
        is_manipulated=True,
        description="Region filled with smooth content (simulated inpainting)",
        image_bytes=_to_jpeg_bytes(img, quality=85),
        manipulation_region={"x": 100, "y": 200, "width": 300, "height": 80},
    )

def gen_inpainting_gradient() -> TestCase:
    """Simulate inpainting with gradient fill."""
    img = _create_base_document()
    arr = np.array(img)
    y1, y2, x1, x2 = 300, 350, 150, 450
    # Linear gradient fill
    for y in range(y1, y2):
        for x in range(x1, x2):
            t = (x - x1) / (x2 - x1)
            arr[y, x] = [int(255 * (1 - t) + 245 * t)] * 3
    img = Image.fromarray(arr)
    return TestCase(
        name="inpainting_gradient",
        category="inpainting",
        is_manipulated=True,
        description="Region filled with linear gradient (simulated inpainting artifact)",
        image_bytes=_to_jpeg_bytes(img, quality=85),
        manipulation_region={"x": 150, "y": 300, "width": 300, "height": 50},
    )

def gen_ai_texture() -> TestCase:
    """Simulate AI-generated texture — uniform noise pattern."""
    img = _create_base_document()
    arr = np.array(img)
    y1, y2, x1, x2 = 150, 350, 50, 545
    # Replace with uniform synthetic texture
    np.random.seed(42)
    synthetic = np.random.randint(240, 255, (y2 - y1, x2 - x1, 3), dtype=np.uint8)
    arr[y1:y2, x1:x2] = synthetic
    img = Image.fromarray(arr)
    return TestCase(
        name="ai_generated_texture",
        category="generation",
        is_manipulated=True,
        description="Large region replaced with synthetic uniform texture",
        image_bytes=_to_jpeg_bytes(img, quality=85),
        manipulation_region={"x": 50, "y": 150, "width": 495, "height": 200},
    )

def gen_resave_edit() -> TestCase:
    """Simulate a re-saved edited file — multiple JPEG compression passes."""
    img = _create_base_document()
    draw = ImageDraw.Draw(img)
    draw.rectangle([200, 310, 400, 340], fill=(255, 252, 245))
    try:
        font = ImageFont.truetype("arial.ttf", 12)
    except (OSError, IOError):
        font = ImageFont.load_default()
    draw.text((297, 325), "Registration No: CS-2024-009999", fill=(40, 40, 40), font=font, anchor="mm")
    # Multiple re-saves at different qualities
    b1 = _to_jpeg_bytes(img, quality=92)
    b2 = _to_jpeg_bytes(Image.open(io.BytesIO(b1)), quality=75)
    b3 = _to_jpeg_bytes(Image.open(io.BytesIO(b2)), quality=60)
    return TestCase(
        name="resave_triple",
        category="resave",
        is_manipulated=True,
        description="Document edited and re-saved 3 times at decreasing quality",
        image_bytes=b3,
        manipulation_region={"x": 200, "y": 310, "width": 200, "height": 30},
    )


# ---------------------------------------------------------------------------
# Robustness degradation transforms
# ---------------------------------------------------------------------------

def _degrade_jpeg(case: TestCase, quality: int) -> TestCase:
    img = Image.open(io.BytesIO(case.image_bytes))
    return TestCase(
        name=f"{case.name}_jpeg_q{quality}",
        category=case.category,
        is_manipulated=case.is_manipulated,
        description=f"{case.description} [degraded: JPEG Q={quality}]",
        image_bytes=_to_jpeg_bytes(img, quality=quality),
        manipulation_region=case.manipulation_region,
    )

def _degrade_blur(case: TestCase, radius: float) -> TestCase:
    img = Image.open(io.BytesIO(case.image_bytes))
    img = img.filter(ImageFilter.GaussianBlur(radius=radius))
    return TestCase(
        name=f"{case.name}_blur_r{radius}",
        category=case.category,
        is_manipulated=case.is_manipulated,
        description=f"{case.description} [degraded: blur r={radius}]",
        image_bytes=_to_jpeg_bytes(img, quality=80),
        manipulation_region=case.manipulation_region,
    )

def _degrade_resize(case: TestCase, scale: float) -> TestCase:
    img = Image.open(io.BytesIO(case.image_bytes))
    new_w, new_h = int(img.width * scale), int(img.height * scale)
    img = img.resize((new_w, new_h), Image.Resampling.LANCZOS)
    return TestCase(
        name=f"{case.name}_resize_{int(scale*100)}pct",
        category=case.category,
        is_manipulated=case.is_manipulated,
        description=f"{case.description} [degraded: resize {int(scale*100)}%]",
        image_bytes=_to_jpeg_bytes(img, quality=80),
        manipulation_region=case.manipulation_region,
    )


# ---------------------------------------------------------------------------
# Test suite assembly
# ---------------------------------------------------------------------------

def build_test_suite() -> list[TestCase]:
    """Build the complete test suite with genuine and manipulated images."""
    cases = []

    # Genuine cases (6)
    cases.append(gen_genuine_clean())
    cases.append(gen_genuine_noisy())
    cases.append(gen_genuine_compressed())
    cases.append(gen_genuine_resized())
    cases.append(gen_genuine_blurred())
    cases.append(gen_genuine_png())

    # Manipulated cases (8)
    cases.append(gen_text_edit())
    cases.append(gen_text_edit_date())
    cases.append(gen_copy_paste())
    cases.append(gen_copy_paste_stamp())
    cases.append(gen_inpainting_smooth())
    cases.append(gen_inpainting_gradient())
    cases.append(gen_ai_texture())
    cases.append(gen_resave_edit())

    return cases


def build_robustness_suite(base_cases: list[TestCase]) -> list[TestCase]:
    """Generate degraded versions of base cases for robustness evaluation."""
    degraded = []
    # Pick a subset of manipulated cases for robustness
    manip_cases = [c for c in base_cases if c.is_manipulated][:4]

    for case in manip_cases:
        # JPEG compression robustness
        degraded.append(_degrade_jpeg(case, quality=30))
        degraded.append(_degrade_jpeg(case, quality=50))
        # Blur robustness
        degraded.append(_degrade_blur(case, radius=2.0))
        # Resize robustness
        degraded.append(_degrade_resize(case, scale=0.5))

    return degraded


# ---------------------------------------------------------------------------
# Module runners
# ---------------------------------------------------------------------------

FORENSICS_THRESHOLD = 0.15   # Risk score above this → predicted manipulated
AI_MANIP_THRESHOLD = 0.20    # Risk score above this → predicted manipulated


def run_forensics(case: TestCase) -> ModuleResult:
    """Run the forensics module on a test case."""
    result = forensics.run(case.image_bytes, case.mime_type)
    risk_score = result.get("riskScore", 0.0) or 0.0
    regions = result.get("suspiciousRegions", [])
    return ModuleResult(
        test_name=case.name,
        module="forensics",
        risk_score=risk_score,
        predicted_manipulated=risk_score > FORENSICS_THRESHOLD,
        regions_detected=len(regions),
        processing_time_ms=result.get("processingTimeMs", 0),
        status=result.get("status", "UNKNOWN"),
        details={"findings_count": len(result.get("findings", []))},
    )


def run_ai_manipulation(case: TestCase) -> ModuleResult:
    """Run the AI manipulation module on a test case."""
    result = ai_manipulation.run(case.image_bytes, case.mime_type)
    risk_score = result.get("riskScore", 0.0) or 0.0
    regions = result.get("suspiciousRegions", [])
    return ModuleResult(
        test_name=case.name,
        module="ai_manipulation",
        risk_score=risk_score,
        predicted_manipulated=risk_score > AI_MANIP_THRESHOLD,
        regions_detected=len(regions),
        processing_time_ms=result.get("processingTimeMs", 0),
        status=result.get("status", "UNKNOWN"),
        details={
            "manipulation_type": result.get("manipulationType", "UNKNOWN"),
            "confidence": result.get("confidence", 0.0),
        },
    )


def run_combined(case: TestCase) -> ModuleResult:
    """Run combined forensics + AI manipulation (simulating evidence fusion)."""
    fr = forensics.run(case.image_bytes, case.mime_type)
    ar = ai_manipulation.run(case.image_bytes, case.mime_type)

    f_score = fr.get("riskScore", 0.0) or 0.0
    a_score = ar.get("riskScore", 0.0) or 0.0

    # Combined score using Phase 10 weights (forensics=0.25, AI=0.25 → equal weight)
    combined_score = (f_score + a_score) / 2.0
    combined_threshold = 0.15

    f_regions = fr.get("suspiciousRegions", [])
    a_regions = ar.get("suspiciousRegions", [])

    return ModuleResult(
        test_name=case.name,
        module="combined",
        risk_score=combined_score,
        predicted_manipulated=combined_score > combined_threshold,
        regions_detected=len(f_regions) + len(a_regions),
        processing_time_ms=(fr.get("processingTimeMs", 0) + ar.get("processingTimeMs", 0)),
        status="SUCCESS" if fr.get("status") == "SUCCESS" and ar.get("status") == "SUCCESS" else "PARTIAL",
        details={
            "forensics_score": f_score,
            "ai_score": a_score,
        },
    )


# ---------------------------------------------------------------------------
# Metrics computation
# ---------------------------------------------------------------------------

def compute_metrics(results: list[ModuleResult], cases: list[TestCase]) -> dict:
    """Compute classification metrics from module results."""
    case_map = {c.name: c for c in cases}

    y_true = []
    y_pred = []
    y_scores = []

    for r in results:
        case = case_map.get(r.test_name)
        if case is None:
            continue
        y_true.append(1 if case.is_manipulated else 0)
        y_pred.append(1 if r.predicted_manipulated else 0)
        y_scores.append(r.risk_score)

    if not y_true:
        return {"error": "No results to evaluate"}

    n = len(y_true)
    tp = sum(1 for t, p in zip(y_true, y_pred) if t == 1 and p == 1)
    fp = sum(1 for t, p in zip(y_true, y_pred) if t == 0 and p == 1)
    fn = sum(1 for t, p in zip(y_true, y_pred) if t == 1 and p == 0)
    tn = sum(1 for t, p in zip(y_true, y_pred) if t == 0 and p == 0)

    accuracy = (tp + tn) / n if n > 0 else 0.0
    precision = tp / (tp + fp) if (tp + fp) > 0 else 0.0
    recall = tp / (tp + fn) if (tp + fn) > 0 else 0.0
    f1 = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0.0

    # ROC-AUC (manual trapezoidal approximation)
    auc = _compute_auc(y_true, y_scores)

    return {
        "n": n,
        "tp": tp, "fp": fp, "fn": fn, "tn": tn,
        "accuracy": round(accuracy, 4),
        "precision": round(precision, 4),
        "recall": round(recall, 4),
        "f1": round(f1, 4),
        "roc_auc": round(auc, 4),
    }


def _compute_auc(y_true: list[int], y_scores: list[float]) -> float:
    """Compute ROC-AUC via trapezoidal rule (no sklearn dependency)."""
    if len(set(y_true)) < 2:
        return 0.5  # Only one class present

    # Sort by score descending
    pairs = sorted(zip(y_scores, y_true), key=lambda x: -x[0])
    total_pos = sum(y_true)
    total_neg = len(y_true) - total_pos

    if total_pos == 0 or total_neg == 0:
        return 0.5

    tp_count = 0
    fp_count = 0
    prev_fpr = 0.0
    prev_tpr = 0.0
    auc = 0.0

    for score, label in pairs:
        if label == 1:
            tp_count += 1
        else:
            fp_count += 1
        tpr = tp_count / total_pos
        fpr = fp_count / total_neg
        # Trapezoidal integration
        auc += (fpr - prev_fpr) * (tpr + prev_tpr) / 2
        prev_fpr = fpr
        prev_tpr = tpr

    return auc


def compute_iou(pred_region: dict, gt_region: dict) -> float:
    """Compute Intersection over Union between two bounding boxes."""
    # Convert to (x1, y1, x2, y2)
    px1 = pred_region["x"]
    py1 = pred_region["y"]
    px2 = px1 + pred_region["width"]
    py2 = py1 + pred_region["height"]

    gx1 = gt_region["x"]
    gy1 = gt_region["y"]
    gx2 = gx1 + gt_region["width"]
    gy2 = gy1 + gt_region["height"]

    # Intersection
    ix1 = max(px1, gx1)
    iy1 = max(py1, gy1)
    ix2 = min(px2, gx2)
    iy2 = min(py2, gy2)

    if ix2 <= ix1 or iy2 <= iy1:
        return 0.0

    intersection = (ix2 - ix1) * (iy2 - iy1)
    area_pred = (px2 - px1) * (py2 - py1)
    area_gt = (gx2 - gx1) * (gy2 - gy1)
    union = area_pred + area_gt - intersection

    return intersection / union if union > 0 else 0.0


# ---------------------------------------------------------------------------
# Main evaluation runner
# ---------------------------------------------------------------------------

def run_evaluation(output_dir: str = "results") -> dict:
    """
    Run the complete Phase 13 evaluation.

    Returns a structured report dict.
    """
    print("=" * 70)
    print("VeriLens Phase 13 — Evaluation Framework")
    print("=" * 70)

    # 1. Build test suites
    print("\n[1/5] Generating test suite...")
    base_suite = build_test_suite()
    robustness_suite = build_robustness_suite(base_suite)
    all_cases = base_suite + robustness_suite

    genuine_count = sum(1 for c in base_suite if not c.is_manipulated)
    manip_count = sum(1 for c in base_suite if c.is_manipulated)
    print(f"  Base: {len(base_suite)} cases ({genuine_count} genuine, {manip_count} manipulated)")
    print(f"  Robustness: {len(robustness_suite)} degraded cases")
    print(f"  Total: {len(all_cases)} test cases")

    # 2. Run modules on base suite
    print("\n[2/5] Running modules on base test suite...")
    forensics_results = []
    ai_results = []
    combined_results = []

    for i, case in enumerate(base_suite):
        print(f"  [{i+1}/{len(base_suite)}] {case.name} ({'MANIP' if case.is_manipulated else 'GENUINE'})...", end=" ")

        fr = run_forensics(case)
        ar = run_ai_manipulation(case)
        cr = run_combined(case)

        forensics_results.append(fr)
        ai_results.append(ar)
        combined_results.append(cr)

        f_ok = "OK" if fr.predicted_manipulated == case.is_manipulated else "XX"
        a_ok = "OK" if ar.predicted_manipulated == case.is_manipulated else "XX"
        c_ok = "OK" if cr.predicted_manipulated == case.is_manipulated else "XX"
        print(f"F:{fr.risk_score:.3f}[{f_ok}] A:{ar.risk_score:.3f}[{a_ok}] C:{cr.risk_score:.3f}[{c_ok}] [{fr.processing_time_ms + ar.processing_time_ms}ms]")

    # 3. Compute base metrics
    print("\n[3/5] Computing classification metrics...")
    base_metrics = {
        "forensics": compute_metrics(forensics_results, base_suite),
        "ai_manipulation": compute_metrics(ai_results, base_suite),
        "combined": compute_metrics(combined_results, base_suite),
    }

    for mod, m in base_metrics.items():
        print(f"  {mod:20s}  Acc={m['accuracy']:.3f}  P={m['precision']:.3f}  R={m['recall']:.3f}  F1={m['f1']:.3f}  AUC={m['roc_auc']:.3f}")

    # 4. Robustness evaluation
    print("\n[4/5] Running robustness evaluation...")
    robustness_forensics = []
    robustness_ai = []
    robustness_combined = []

    for i, case in enumerate(robustness_suite):
        fr = run_forensics(case)
        ar = run_ai_manipulation(case)
        cr = run_combined(case)
        robustness_forensics.append(fr)
        robustness_ai.append(ar)
        robustness_combined.append(cr)
        print(f"  [{i+1}/{len(robustness_suite)}] {case.name}: F={fr.risk_score:.3f} A={ar.risk_score:.3f} C={cr.risk_score:.3f}")

    robustness_metrics = {
        "forensics": compute_metrics(robustness_forensics, robustness_suite),
        "ai_manipulation": compute_metrics(robustness_ai, robustness_suite),
        "combined": compute_metrics(robustness_combined, robustness_suite),
    }

    print("\n  Robustness metrics (degraded manipulated images):")
    for mod, m in robustness_metrics.items():
        print(f"  {mod:20s}  Acc={m.get('accuracy',0):.3f}  R={m.get('recall',0):.3f}  F1={m.get('f1',0):.3f}")

    # 5. Localization (IoU) evaluation
    print("\n[5/5] Computing localization IoU...")
    iou_scores = []
    for case, fr, ar in zip(base_suite, forensics_results, ai_results):
        if case.manipulation_region and case.is_manipulated:
            gt = case.manipulation_region
            # Check forensics regions
            f_result = forensics.run(case.image_bytes, case.mime_type)
            for region in f_result.get("suspiciousRegions", []):
                iou = compute_iou(region, gt)
                if iou > 0:
                    iou_scores.append({"case": case.name, "source": "forensics", "iou": round(iou, 4)})
            # Check AI manipulation regions
            a_result = ai_manipulation.run(case.image_bytes, case.mime_type)
            for region in a_result.get("suspiciousRegions", []):
                iou = compute_iou(region, gt)
                if iou > 0:
                    iou_scores.append({"case": case.name, "source": "ai_manipulation", "iou": round(iou, 4)})

    avg_iou = np.mean([s["iou"] for s in iou_scores]) if iou_scores else 0.0
    print(f"  Overlapping regions found: {len(iou_scores)}")
    print(f"  Average IoU: {avg_iou:.4f}")

    # Compile report
    report = {
        "framework": "VeriLens Phase 13 Evaluation",
        "version": "1.0.0",
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S"),
        "test_suite": {
            "base_cases": len(base_suite),
            "genuine": genuine_count,
            "manipulated": manip_count,
            "robustness_cases": len(robustness_suite),
            "total": len(all_cases),
            "categories": list(set(c.category for c in all_cases)),
        },
        "thresholds": {
            "forensics": FORENSICS_THRESHOLD,
            "ai_manipulation": AI_MANIP_THRESHOLD,
            "combined": 0.15,
        },
        "base_metrics": base_metrics,
        "robustness_metrics": robustness_metrics,
        "localization": {
            "overlapping_regions": len(iou_scores),
            "average_iou": round(float(avg_iou), 4),
            "details": iou_scores[:20],
        },
        "per_case_results": {
            "forensics": [asdict(r) for r in forensics_results],
            "ai_manipulation": [asdict(r) for r in ai_results],
            "combined": [asdict(r) for r in combined_results],
        },
        "robustness_results": {
            "forensics": [asdict(r) for r in robustness_forensics],
            "ai_manipulation": [asdict(r) for r in robustness_ai],
            "combined": [asdict(r) for r in robustness_combined],
        },
        "limitations": [
            "Evaluation uses synthetic test images, not real manipulated documents",
            "Test suite is small (14 base + 16 robustness) — results are indicative, not definitive",
            "Genuine/manipulated distribution may not match real-world prevalence",
            "IoU evaluation is limited by synthetic ground-truth bounding boxes",
            "Deep learning models (ArcFace, manipulation classifiers) not yet integrated",
            "OCR, metadata, identity, and face modules are not evaluated for classification accuracy here (they contribute evidence, not binary predictions)",
        ],
        "recommendations": [
            "Collect or license a real manipulated-document dataset for Phase 14+ validation",
            "Tune thresholds based on precision/recall tradeoff requirements",
            "Consider deep learning baselines (ManTraNet, CAT-Net) for comparison in future work",
            "Evaluate fusion weights with larger, more diverse test sets",
            "Add cross-format evaluation (PDF vs scanned image vs mobile photo)",
        ],
    }

    # Save report
    os.makedirs(output_dir, exist_ok=True)
    report_path = os.path.join(output_dir, "evaluation_report.json")
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2, ensure_ascii=False)
    print(f"\n[OK] Report saved to {report_path}")

    # Save human-readable summary
    summary_path = os.path.join(output_dir, "evaluation_summary.md")
    with open(summary_path, "w", encoding="utf-8") as f:
        f.write(_format_markdown_summary(report))
    print(f"[OK] Summary saved to {summary_path}")

    print("\n" + "=" * 70)
    print("Evaluation complete.")
    print("=" * 70)

    return report


def _format_markdown_summary(report: dict) -> str:
    """Generate a markdown summary of the evaluation report."""
    lines = [
        "# VeriLens — Evaluation Report (Phase 13)",
        "",
        f"**Generated:** {report['timestamp']}  ",
        f"**Version:** {report['version']}",
        "",
        "---",
        "",
        "## 1. Test Suite Composition",
        "",
        f"| Category | Count |",
        f"| -------- | ----- |",
        f"| Genuine  | {report['test_suite']['genuine']} |",
        f"| Manipulated | {report['test_suite']['manipulated']} |",
        f"| Robustness (degraded) | {report['test_suite']['robustness_cases']} |",
        f"| **Total** | **{report['test_suite']['total']}** |",
        "",
        f"Manipulation categories: {', '.join(report['test_suite']['categories'])}",
        "",
        "> [!NOTE]",
        "> This evaluation uses **synthetic test images** with programmatically applied manipulations.",
        "> Results represent benchmark performance, not real-world forensic accuracy.",
        "",
        "---",
        "",
        "## 2. Classification Metrics — Base Test Suite",
        "",
        "| Module | Accuracy | Precision | Recall | F1 | ROC-AUC |",
        "| ------ | -------- | --------- | ------ | -- | ------- |",
    ]

    for mod in ["forensics", "ai_manipulation", "combined"]:
        m = report["base_metrics"][mod]
        lines.append(f"| {mod} | {m['accuracy']:.3f} | {m['precision']:.3f} | {m['recall']:.3f} | {m['f1']:.3f} | {m['roc_auc']:.3f} |")

    lines += [
        "",
        "**Thresholds used:**",
        f"- Forensics: risk score > {report['thresholds']['forensics']}",
        f"- AI Manipulation: risk score > {report['thresholds']['ai_manipulation']}",
        f"- Combined: average score > {report['thresholds']['combined']}",
        "",
        "---",
        "",
        "## 3. Robustness Metrics — Degraded Images",
        "",
        "Tests module resilience to: JPEG compression (Q=30, Q=50), Gaussian blur (r=2.0), resize (50%).",
        "",
        "| Module | Accuracy | Recall | F1 |",
        "| ------ | -------- | ------ | -- |",
    ]

    for mod in ["forensics", "ai_manipulation", "combined"]:
        m = report["robustness_metrics"][mod]
        lines.append(f"| {mod} | {m.get('accuracy',0):.3f} | {m.get('recall',0):.3f} | {m.get('f1',0):.3f} |")

    lines += [
        "",
        "---",
        "",
        "## 4. Localization — IoU Evaluation",
        "",
        f"- Overlapping regions found: **{report['localization']['overlapping_regions']}**",
        f"- Average IoU: **{report['localization']['average_iou']:.4f}**",
        "",
        "> [!TIP]",
        "> IoU measures how well detected suspicious regions overlap with known manipulation regions.",
        "> Higher values indicate better spatial localization.",
        "",
        "---",
        "",
        "## 5. Limitations",
        "",
    ]

    for lim in report["limitations"]:
        lines.append(f"- {lim}")

    lines += [
        "",
        "---",
        "",
        "## 6. Recommendations",
        "",
    ]

    for rec in report["recommendations"]:
        lines.append(f"- {rec}")

    lines += [
        "",
        "---",
        "",
        "> [!IMPORTANT]",
        "> VeriLens is an **evidence-first screening system**. It does not claim 100% detection accuracy.",
        "> These evaluation results should be interpreted as baseline benchmarks for the current",
        "> heuristic-based approach. Deep learning models and real-world datasets are needed for",
        "> production-grade forensic accuracy assessment.",
        "",
    ]

    return "\n".join(lines)


# ---------------------------------------------------------------------------
# CLI entry point
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="VeriLens Phase 13 Evaluation")
    parser.add_argument("--output-dir", default="evaluation/results", help="Output directory for results")
    args = parser.parse_args()
    run_evaluation(args.output_dir)
