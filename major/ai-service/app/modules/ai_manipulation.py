"""
AI Manipulation Screening Module — Phase 9.

Screens for possible AI-assisted manipulation using explainable,
frequency-domain and statistical techniques. No deep learning model
is required for this baseline.

Techniques:
  1. Spectral Analysis (FFT)  — AI-generated images exhibit distinct
     patterns in the frequency domain (e.g., reduced high-frequency
     energy, spectral peaks from GAN grid artifacts).
  2. JPEG Ghost Detection    — Re-compression at varying quality levels
     reveals regions saved at a different quality than the rest,
     indicating cut-and-paste from a different source.
  3. Gradient Consistency     — AI inpainting often produces unnaturally
     smooth gradients; measure local gradient magnitude variance.
  4. Texture Regularity       — AI-generated textures show statistical
     regularities; measure Local Binary Pattern (LBP) uniformity.

IMPORTANT:
  - AI detection is NOT 100% reliable.
  - These techniques produce evidence, not proof.
  - Results use careful language: "potential manipulation detected",
    "elevated risk", never "definitely AI-generated".
  - Degraded images (heavy compression, screenshots) reduce accuracy.
  - Limitations are documented honestly.
"""

import io
import time
import math
import numpy as np

try:
    import cv2
    OPENCV_AVAILABLE = True
except ImportError:
    OPENCV_AVAILABLE = False

from PIL import Image


# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

# Thresholds — configurable, initial estimates subject to Phase 13 validation.
SPECTRAL_HIGH_FREQ_THRESHOLD = 0.15       # Ratio of high-freq energy below this → suspicious
JPEG_GHOST_THRESHOLD = 12.0               # Mean absolute error above this → ghost detected
GRADIENT_SMOOTHNESS_THRESHOLD = 0.25      # Gradient variance ratio below this → suspiciously smooth
TEXTURE_UNIFORMITY_THRESHOLD = 0.40       # LBP uniformity above this → suspiciously uniform

# Severity weights for risk score calculation
WEIGHTS = {
    "spectral_anomaly":     {"weight": 0.30, "severity": "HIGH"},
    "jpeg_ghost":           {"weight": 0.25, "severity": "MEDIUM"},
    "gradient_smoothness":  {"weight": 0.25, "severity": "MEDIUM"},
    "texture_regularity":   {"weight": 0.20, "severity": "LOW"},
}

MODEL_INFO = {
    "name": "VeriLens Frequency-Statistical Baseline",
    "version": "1.0.0",
    "techniques": [
        "Spectral Analysis (FFT)",
        "JPEG Ghost Detection",
        "Gradient Consistency Analysis",
        "Texture Regularity (LBP)",
    ],
    "limitations": [
        "Not trained on specific manipulation datasets — uses statistical heuristics",
        "Heavy JPEG compression reduces spectral analysis accuracy",
        "Screenshots and camera re-captures add noise that masks signals",
        "AI-generated images from latest models may evade detection",
        "Results are screening signals, not forensic proof",
    ],
}


def is_available() -> bool:
    """Return True if OpenCV is available for AI manipulation screening."""
    return OPENCV_AVAILABLE


# ---------------------------------------------------------------------------
# Image loading
# ---------------------------------------------------------------------------

def _load_image(file_bytes: bytes, mime_type: str = None) -> np.ndarray | None:
    """Load image bytes into an OpenCV BGR array."""
    try:
        if mime_type == "application/pdf":
            try:
                from pdf2image import convert_from_bytes
                pages = convert_from_bytes(file_bytes, dpi=200, first_page=1, last_page=1)
                if not pages:
                    return None
                pil_img = pages[0].convert("RGB")
                arr = np.array(pil_img)
                return cv2.cvtColor(arr, cv2.COLOR_RGB2BGR)
            except Exception:
                return None
        else:
            arr = np.frombuffer(file_bytes, dtype=np.uint8)
            img = cv2.imdecode(arr, cv2.IMREAD_COLOR)
            if img is not None:
                return img
            # Fallback to PIL
            pil = Image.open(io.BytesIO(file_bytes)).convert("RGB")
            return cv2.cvtColor(np.array(pil), cv2.COLOR_RGB2BGR)
    except Exception:
        return None


# ---------------------------------------------------------------------------
# Technique 1: Spectral Analysis (FFT)
# ---------------------------------------------------------------------------

def _spectral_analysis(gray: np.ndarray) -> dict:
    """
    Analyze the frequency spectrum of the image.

    AI-generated images (especially GAN-produced) tend to have:
    - Reduced high-frequency energy compared to natural images
    - Periodic peaks from convolutional grid artifacts
    - Unnaturally smooth spectral falloff

    Returns a finding dict if anomalies are detected.
    """
    # Compute 2D FFT
    f_transform = np.fft.fft2(gray.astype(np.float32))
    f_shift = np.fft.fftshift(f_transform)
    magnitude = np.abs(f_shift)
    magnitude = np.log1p(magnitude)  # Log scale for better dynamic range

    h, w = gray.shape
    cy, cx = h // 2, w // 2

    # Define frequency bands
    total_energy = magnitude.sum() + 1e-8

    # High frequency: outer 25% of the spectrum
    radius_high = min(cy, cx) * 0.75
    y_coords, x_coords = np.ogrid[:h, :w]
    dist = np.sqrt((y_coords - cy) ** 2 + (x_coords - cx) ** 2)

    high_freq_mask = dist >= radius_high
    high_freq_energy = magnitude[high_freq_mask].sum()
    high_freq_ratio = float(high_freq_energy / total_energy)

    # Low frequency: inner 25% of the spectrum
    radius_low = min(cy, cx) * 0.25
    low_freq_mask = dist <= radius_low
    low_freq_energy = magnitude[low_freq_mask].sum()
    low_freq_ratio = float(low_freq_energy / total_energy)

    # Check for spectral anomalies
    is_suspicious = bool(high_freq_ratio < SPECTRAL_HIGH_FREQ_THRESHOLD)

    # Check for periodic peaks (GAN artifacts) in mid-frequency band
    mid_mask = (dist > radius_low) & (dist < radius_high)
    mid_spectrum = magnitude.copy()
    mid_spectrum[~mid_mask] = 0
    mid_mean = float(mid_spectrum[mid_mask].mean()) if mid_mask.any() else 0.0
    mid_std = float(mid_spectrum[mid_mask].std()) if mid_mask.any() else 0.0

    # Count peaks significantly above mean (potential GAN grid artifacts)
    peak_threshold = mid_mean + 3 * mid_std if mid_std > 0 else mid_mean * 2
    periodic_peaks = int(np.sum(mid_spectrum > peak_threshold)) if peak_threshold > 0 else 0
    has_periodic_peaks = bool(periodic_peaks > 20)  # Threshold for suspicious periodic structure

    result = {
        "technique": "spectral_analysis",
        "highFreqRatio": round(float(high_freq_ratio), 4),
        "lowFreqRatio": round(float(low_freq_ratio), 4),
        "periodicPeaks": int(periodic_peaks),
        "suspicious": bool(is_suspicious or has_periodic_peaks),
    }

    if is_suspicious:
        result["description"] = (
            f"Reduced high-frequency energy ({high_freq_ratio:.3f} < {SPECTRAL_HIGH_FREQ_THRESHOLD}) "
            "may indicate AI-generated or heavily processed content"
        )
        result["confidence"] = round(min(0.95, (SPECTRAL_HIGH_FREQ_THRESHOLD - high_freq_ratio) / SPECTRAL_HIGH_FREQ_THRESHOLD + 0.3), 2)
    elif has_periodic_peaks:
        result["description"] = (
            f"Detected {periodic_peaks} periodic spectral peaks — "
            "may indicate GAN-generated content or grid-based processing artifacts"
        )
        result["confidence"] = round(min(0.85, 0.4 + periodic_peaks / 200), 2)
    else:
        result["description"] = "No significant spectral anomalies detected"
        result["confidence"] = 0.0

    return result


# ---------------------------------------------------------------------------
# Technique 2: JPEG Ghost Detection
# ---------------------------------------------------------------------------

def _jpeg_ghost_detection(img: np.ndarray) -> dict:
    """
    Detect regions that were saved at a different JPEG quality.

    If a region was pasted from an image saved at quality Q1 into an image
    saved at Q2, re-saving at Q1 will show minimal error in the pasted region
    but high error elsewhere (or vice versa).

    This indicates copy-paste manipulation from a different source.
    """
    # Test several JPEG quality levels
    quality_levels = [50, 60, 70, 80, 90]
    ghost_findings = []
    block_size = 64

    h, w = img.shape[:2]
    if h < block_size or w < block_size:
        return {
            "technique": "jpeg_ghost",
            "suspicious": False,
            "description": "Image too small for JPEG ghost analysis",
            "confidence": 0.0,
            "ghostRegions": [],
        }

    best_quality = None
    best_variance = 0.0
    best_ghost_blocks = []

    for quality in quality_levels:
        # Re-compress at this quality
        encode_params = [cv2.IMWRITE_JPEG_QUALITY, quality]
        _, encoded = cv2.imencode(".jpg", img, encode_params)
        recompressed = cv2.imdecode(encoded, cv2.IMREAD_COLOR)

        if recompressed is None:
            continue

        # Compute per-pixel absolute difference
        diff = cv2.absdiff(img, recompressed).astype(np.float32)
        diff_gray = np.mean(diff, axis=2)

        # Block-level analysis
        block_errors = []
        for by in range(0, h - block_size + 1, block_size):
            for bx in range(0, w - block_size + 1, block_size):
                block = diff_gray[by:by + block_size, bx:bx + block_size]
                block_mean = block.mean()
                block_errors.append((bx, by, float(block_mean)))

        if not block_errors:
            continue

        errors = [e[2] for e in block_errors]
        mean_error = np.mean(errors)
        std_error = np.std(errors)
        variance = float(std_error / (mean_error + 1e-8))

        if variance > best_variance:
            best_variance = variance
            best_quality = quality
            # Find blocks with significantly different error
            threshold = mean_error + 2 * std_error
            ghost_blocks = [
                {"x": int(bx), "y": int(by), "width": block_size, "height": block_size,
                 "error": round(err, 2)}
                for bx, by, err in block_errors
                if abs(err - mean_error) > 2 * std_error
            ]
            best_ghost_blocks = ghost_blocks[:10]  # Cap regions

    is_suspicious = bool(best_variance > 1.5 and len(best_ghost_blocks) > 0)

    result = {
        "technique": "jpeg_ghost",
        "suspicious": is_suspicious,
        "bestQuality": int(best_quality) if best_quality is not None else None,
        "errorVariance": round(float(best_variance), 4),
        "ghostRegions": best_ghost_blocks,
    }

    if is_suspicious:
        result["description"] = (
            f"JPEG ghost analysis at quality {best_quality} reveals {len(best_ghost_blocks)} "
            "regions with inconsistent compression history — may indicate copy-paste manipulation"
        )
        result["confidence"] = round(min(0.90, 0.3 + best_variance / 5), 2)
    else:
        result["description"] = "No significant JPEG ghost artifacts detected"
        result["confidence"] = 0.0

    return result


# ---------------------------------------------------------------------------
# Technique 3: Gradient Consistency Analysis
# ---------------------------------------------------------------------------

def _gradient_consistency(gray: np.ndarray) -> dict:
    """
    Analyze gradient magnitude variance across the image.

    AI inpainting often produces unnaturally smooth gradients in the
    manipulated region, creating a measurable contrast with natural
    image texture.

    We divide the image into blocks and compare gradient magnitude
    variance. Blocks with significantly lower variance than the
    overall image may indicate AI-generated content.
    """
    # Compute Sobel gradients
    gx = cv2.Sobel(gray, cv2.CV_32F, 1, 0, ksize=3)
    gy = cv2.Sobel(gray, cv2.CV_32F, 0, 1, ksize=3)
    gradient_mag = np.sqrt(gx ** 2 + gy ** 2)

    h, w = gray.shape
    block_size = 32
    if h < block_size * 2 or w < block_size * 2:
        return {
            "technique": "gradient_consistency",
            "suspicious": False,
            "description": "Image too small for gradient consistency analysis",
            "confidence": 0.0,
            "smoothBlocks": 0,
            "totalBlocks": 0,
        }

    # Global gradient statistics
    global_var = float(np.var(gradient_mag))
    if global_var < 1e-6:
        return {
            "technique": "gradient_consistency",
            "suspicious": False,
            "description": "Image has near-zero gradient variance (possibly blank or uniform)",
            "confidence": 0.0,
            "smoothBlocks": 0,
            "totalBlocks": 0,
        }

    # Per-block gradient variance
    block_vars = []
    smooth_blocks = []
    total_blocks = 0

    for by in range(0, h - block_size + 1, block_size):
        for bx in range(0, w - block_size + 1, block_size):
            block = gradient_mag[by:by + block_size, bx:bx + block_size]
            bvar = float(np.var(block))
            ratio = bvar / global_var
            block_vars.append(ratio)
            total_blocks += 1

            if ratio < GRADIENT_SMOOTHNESS_THRESHOLD:
                smooth_blocks.append({
                    "x": int(bx), "y": int(by),
                    "width": block_size, "height": block_size,
                    "varianceRatio": round(ratio, 4),
                })

    smooth_ratio = len(smooth_blocks) / total_blocks if total_blocks > 0 else 0

    # Suspicious if >15% of blocks are unnaturally smooth
    is_suspicious = bool(smooth_ratio > 0.15 and len(smooth_blocks) >= 3)

    result = {
        "technique": "gradient_consistency",
        "suspicious": is_suspicious,
        "smoothBlocks": int(len(smooth_blocks)),
        "totalBlocks": int(total_blocks),
        "smoothRatio": round(float(smooth_ratio), 4),
        "smoothRegions": smooth_blocks[:10],  # Cap at 10
    }

    if is_suspicious:
        result["description"] = (
            f"{len(smooth_blocks)}/{total_blocks} blocks ({smooth_ratio:.1%}) show unnaturally "
            "smooth gradients — may indicate AI inpainting or generation"
        )
        result["confidence"] = round(min(0.85, 0.3 + float(smooth_ratio)), 2)
    else:
        result["description"] = "Gradient consistency appears natural"
        result["confidence"] = 0.0

    return result


# ---------------------------------------------------------------------------
# Technique 4: Texture Regularity (LBP-based)
# ---------------------------------------------------------------------------

def _texture_regularity(gray: np.ndarray) -> dict:
    """
    Analyze texture regularity using Local Binary Pattern statistics.

    AI-generated textures tend to show higher statistical uniformity
    in LBP histograms compared to natural images, which have more
    varied local texture patterns.

    We compute a simplified LBP and measure histogram uniformity.
    """
    h, w = gray.shape
    if h < 10 or w < 10:
        return {
            "technique": "texture_regularity",
            "suspicious": False,
            "description": "Image too small for texture regularity analysis",
            "confidence": 0.0,
            "uniformity": 0.0,
        }

    # Simplified LBP computation (8 neighbors, radius 1)
    # Compare each pixel with its 8 neighbors
    padded = cv2.copyMakeBorder(gray, 1, 1, 1, 1, cv2.BORDER_REFLECT)
    lbp = np.zeros_like(gray, dtype=np.uint8)

    # 8 neighbors: top-left, top, top-right, right, bottom-right, bottom, bottom-left, left
    offsets = [(-1, -1), (-1, 0), (-1, 1), (0, 1), (1, 1), (1, 0), (1, -1), (0, -1)]

    for bit, (dy, dx) in enumerate(offsets):
        neighbor = padded[1 + dy:h + 1 + dy, 1 + dx:w + 1 + dx]
        lbp |= ((neighbor >= gray).astype(np.uint8) << bit)

    # Compute LBP histogram
    hist, _ = np.histogram(lbp, bins=256, range=(0, 256))
    hist = hist.astype(np.float32)
    hist_norm = hist / (hist.sum() + 1e-8)

    # Uniformity metric (sum of squared probabilities)
    # Higher uniformity → more regular texture → potentially AI-generated
    uniformity = float(np.sum(hist_norm ** 2))

    # Entropy (lower entropy → more regular)
    entropy = float(-np.sum(hist_norm * np.log2(hist_norm + 1e-10)))

    # Block-level uniformity analysis
    block_size = 64
    block_uniformities = []
    for by in range(0, h - block_size + 1, block_size):
        for bx in range(0, w - block_size + 1, block_size):
            block = lbp[by:by + block_size, bx:bx + block_size]
            bhist, _ = np.histogram(block, bins=256, range=(0, 256))
            bhist = bhist.astype(np.float32)
            bhist_norm = bhist / (bhist.sum() + 1e-8)
            bu = float(np.sum(bhist_norm ** 2))
            block_uniformities.append(bu)

    avg_block_uniformity = float(np.mean(block_uniformities)) if block_uniformities else float(uniformity)
    uniformity_std = float(np.std(block_uniformities)) if block_uniformities else 0.0

    is_suspicious = bool(avg_block_uniformity > TEXTURE_UNIFORMITY_THRESHOLD)

    result = {
        "technique": "texture_regularity",
        "suspicious": is_suspicious,
        "globalUniformity": round(float(uniformity), 4),
        "avgBlockUniformity": round(float(avg_block_uniformity), 4),
        "uniformityStd": round(float(uniformity_std), 4),
        "entropy": round(float(entropy), 4),
    }

    if is_suspicious:
        result["description"] = (
            f"Texture uniformity ({avg_block_uniformity:.3f} > {TEXTURE_UNIFORMITY_THRESHOLD}) "
            "is higher than expected for natural images — may indicate AI generation"
        )
        result["confidence"] = round(min(0.80, 0.3 + (avg_block_uniformity - TEXTURE_UNIFORMITY_THRESHOLD) * 2), 2)
    else:
        result["description"] = "Texture regularity appears natural"
        result["confidence"] = 0.0

    return result


# ---------------------------------------------------------------------------
# Risk Score Calculation
# ---------------------------------------------------------------------------

def _calculate_risk_score(findings: list[dict]) -> tuple[float, str]:
    """
    Compute a weighted risk score from technique findings.

    Returns (score, manipulation_type).
    Score is 0.0–1.0; manipulation_type is inferred from findings.
    """
    if not findings:
        return 0.0, "NONE_DETECTED"

    total_weight = 0.0
    weighted_score = 0.0
    suspicious_techniques = []

    for finding in findings:
        technique = finding.get("technique", "")
        is_suspicious = finding.get("suspicious", False)
        confidence = finding.get("confidence", 0.0)
        weight_info = WEIGHTS.get(technique, {"weight": 0.1})

        w = weight_info["weight"]
        total_weight += w

        if is_suspicious:
            weighted_score += w * confidence
            suspicious_techniques.append(technique)

    score = weighted_score / total_weight if total_weight > 0 else 0.0
    score = round(min(1.0, score), 4)

    # Determine manipulation type
    if not suspicious_techniques:
        manipulation_type = "NONE_DETECTED"
    elif "jpeg_ghost" in suspicious_techniques:
        manipulation_type = "SPLICING"
    elif "gradient_consistency" in suspicious_techniques and "spectral_analysis" in suspicious_techniques:
        manipulation_type = "INPAINTING"
    elif "spectral_analysis" in suspicious_techniques and "texture_regularity" in suspicious_techniques:
        manipulation_type = "GENERATION"
    else:
        manipulation_type = "UNKNOWN"

    return score, manipulation_type


# ---------------------------------------------------------------------------
# Suspicious Regions
# ---------------------------------------------------------------------------

def _collect_suspicious_regions(findings: list[dict]) -> list[dict]:
    """
    Collect suspicious regions from all techniques that provide spatial info.
    Cap at 15 regions total.
    """
    regions = []

    for finding in findings:
        if not finding.get("suspicious", False):
            continue

        technique = finding.get("technique", "UNKNOWN")
        source_label = technique.upper().replace("_", " ")

        # JPEG ghost regions
        for ghost in finding.get("ghostRegions", []):
            regions.append({
                "x": ghost["x"],
                "y": ghost["y"],
                "width": ghost["width"],
                "height": ghost["height"],
                "source": f"AI_MANIPULATION_{source_label}",
                "confidence": finding.get("confidence", 0.5),
                "description": f"JPEG ghost artifact detected (error: {ghost.get('error', 'N/A')})",
            })

        # Gradient smooth regions
        for smooth in finding.get("smoothRegions", []):
            regions.append({
                "x": smooth["x"],
                "y": smooth["y"],
                "width": smooth["width"],
                "height": smooth["height"],
                "source": f"AI_MANIPULATION_{source_label}",
                "confidence": finding.get("confidence", 0.5),
                "description": f"Unnaturally smooth gradient (variance ratio: {smooth.get('varianceRatio', 'N/A')})",
            })

    # Cap and sort by confidence descending
    regions.sort(key=lambda r: r.get("confidence", 0), reverse=True)
    return regions[:15]


# ---------------------------------------------------------------------------
# Public entry point
# ---------------------------------------------------------------------------

def run(file_bytes: bytes, mime_type: str) -> dict:
    """
    Run AI manipulation screening.

    Args:
        file_bytes: Document file bytes.
        mime_type:  Document MIME type.

    Returns:
        AI manipulation screening result dict matching the VeriLens schema.
    """
    start = time.time()

    if not is_available():
        return {
            "status": "FAILED",
            "error": "OpenCV not available for AI manipulation screening",
            "riskScore": None,
            "manipulationType": "UNKNOWN",
            "confidence": 0.0,
            "findings": [],
            "suspiciousRegions": [],
            "modelInfo": MODEL_INFO,
            "processingTimeMs": int((time.time() - start) * 1000),
        }

    # Load image
    img = _load_image(file_bytes, mime_type)
    if img is None:
        return {
            "status": "FAILED",
            "error": "Could not decode image for AI manipulation screening",
            "riskScore": None,
            "manipulationType": "UNKNOWN",
            "confidence": 0.0,
            "findings": [],
            "suspiciousRegions": [],
            "modelInfo": MODEL_INFO,
            "processingTimeMs": int((time.time() - start) * 1000),
        }

    # Convert to grayscale for analysis
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    # Run all four techniques
    findings = []

    try:
        findings.append(_spectral_analysis(gray))
    except Exception as e:
        findings.append({
            "technique": "spectral_analysis",
            "suspicious": False,
            "description": f"Spectral analysis failed: {str(e)}",
            "confidence": 0.0,
        })

    try:
        findings.append(_jpeg_ghost_detection(img))
    except Exception as e:
        findings.append({
            "technique": "jpeg_ghost",
            "suspicious": False,
            "description": f"JPEG ghost detection failed: {str(e)}",
            "confidence": 0.0,
        })

    try:
        findings.append(_gradient_consistency(gray))
    except Exception as e:
        findings.append({
            "technique": "gradient_consistency",
            "suspicious": False,
            "description": f"Gradient consistency analysis failed: {str(e)}",
            "confidence": 0.0,
        })

    try:
        findings.append(_texture_regularity(gray))
    except Exception as e:
        findings.append({
            "technique": "texture_regularity",
            "suspicious": False,
            "description": f"Texture regularity analysis failed: {str(e)}",
            "confidence": 0.0,
        })

    # Calculate risk score and manipulation type
    risk_score, manipulation_type = _calculate_risk_score(findings)

    # Overall confidence is the max confidence of suspicious findings
    suspicious_confidences = [f.get("confidence", 0.0) for f in findings if f.get("suspicious")]
    overall_confidence = max(suspicious_confidences) if suspicious_confidences else 0.0

    # Collect suspicious regions
    suspicious_regions = _collect_suspicious_regions(findings)

    # Format findings for output
    formatted_findings = []
    for f in findings:
        severity_key = f.get("technique", "")
        severity = WEIGHTS.get(severity_key, {}).get("severity", "LOW") if f.get("suspicious") else "LOW"

        formatted_findings.append({
            "type": f.get("technique", "unknown"),
            "severity": severity if f.get("suspicious") else "NONE",
            "description": f.get("description", ""),
            "confidence": f.get("confidence", 0.0),
            "suspicious": f.get("suspicious", False),
            "details": {k: v for k, v in f.items()
                        if k not in ("technique", "description", "confidence",
                                     "suspicious", "ghostRegions", "smoothRegions")},
        })

    processing_time = int((time.time() - start) * 1000)

    return {
        "status": "SUCCESS",
        "riskScore": round(risk_score, 4),
        "manipulationType": manipulation_type,
        "confidence": round(overall_confidence, 4),
        "findings": formatted_findings,
        "suspiciousRegions": suspicious_regions,
        "modelInfo": MODEL_INFO,
        "processingTimeMs": processing_time,
    }
