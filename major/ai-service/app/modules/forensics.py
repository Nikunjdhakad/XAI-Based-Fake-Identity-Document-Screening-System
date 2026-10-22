"""
Image Forensics Module — Phase 7.

Implements explainable baseline forensic techniques using OpenCV.
No deep learning models — all techniques are interpretable and produce
visualisable evidence.

Techniques implemented:
  1. Error Level Analysis (ELA) — re-save at known quality, detect regions
     with different compression error (potential manipulation indicators)
  2. Noise inconsistency — local noise variance via Laplacian, detect
     regions with anomalous noise patterns (splicing indicators)
  3. Edge density analysis — Canny edge detection, detect regions with
     unusual edge density (copy-paste / inpainting indicators)

Important: These are indicators, not proof of forgery.
OpenCV alone does not prove manipulation — findings are labelled as
"potential indicators" only.
"""

import io
import time
import numpy as np
from PIL import Image

try:
    import cv2
    OPENCV_AVAILABLE = True
except ImportError:
    OPENCV_AVAILABLE = False


def is_available() -> bool:
    """Return True if OpenCV is installed."""
    return OPENCV_AVAILABLE


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _to_cv2_image(file_bytes: bytes, mime_type: str) -> np.ndarray | None:
    """Convert file bytes to an OpenCV BGR image. Returns None on failure."""
    try:
        if mime_type == "application/pdf":
            # For PDFs, convert first page to image using pdf2image
            from pdf2image import convert_from_bytes
            pages = convert_from_bytes(file_bytes, dpi=200, first_page=1, last_page=1)
            if not pages:
                return None
            pil_img = pages[0].convert("RGB")
            arr = np.array(pil_img)
            return cv2.cvtColor(arr, cv2.COLOR_RGB2BGR)
        else:
            # Decode image from bytes
            arr = np.frombuffer(file_bytes, dtype=np.uint8)
            return cv2.imdecode(arr, cv2.IMREAD_COLOR)
    except Exception:
        return None


def _gray(img: np.ndarray) -> np.ndarray:
    return cv2.cvtColor(img, cv2.COLOR_BGR2GRAY) if len(img.shape) == 3 else img


# ---------------------------------------------------------------------------
# 1. Error Level Analysis (ELA)
# ---------------------------------------------------------------------------

def _error_level_analysis(img: np.ndarray, quality: int = 90) -> dict:
    """
    Re-save the image at a known JPEG quality and compute the pixel-level
    difference. Regions with higher error levels may indicate manipulation.

    Returns dict with: ela_map (normalised difference), mean_error, max_region.
    """
    # Encode original as JPEG at the given quality
    encode_params = [int(cv2.IMWRITE_JPEG_QUALITY), quality]
    _, encoded = cv2.imencode(".jpg", img, encode_params)
    redecoded = cv2.imdecode(encoded, cv2.IMREAD_COLOR)

    # Compute absolute difference
    diff = cv2.absdiff(img, redecoded)
    gray_diff = _gray(diff).astype(np.float32)

    # Normalise to 0-255 for thresholding
    max_val = max(gray_diff.max(), 1.0)
    ela_norm = (gray_diff / max_val * 255).astype(np.uint8)

    mean_error = float(gray_diff.mean())

    # Threshold to find high-error regions
    _, thresh = cv2.threshold(ela_norm, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    suspicious_regions = []
    for contour in contours:
        area = cv2.contourArea(contour)
        if area < 100:  # Skip tiny regions
            continue
        x, y, w, h = cv2.boundingRect(contour)
        # Compute local mean error in this region
        local = gray_diff[y:y+h, x:x+w]
        local_mean = float(local.mean()) if local.size > 0 else 0.0
        confidence = min(local_mean / 50.0, 1.0)  # Heuristic normalisation

        suspicious_regions.append({
            "x": int(x), "y": int(y), "width": int(w), "height": int(h),
            "confidence": round(confidence, 4),
            "description": "High error level region — potential editing indicator",
        })

    # Sort by confidence (descending), take top 10
    suspicious_regions.sort(key=lambda r: r["confidence"], reverse=True)

    return {
        "meanError": round(mean_error, 4),
        "regions": suspicious_regions[:10],
    }


# ---------------------------------------------------------------------------
# 2. Noise inconsistency analysis
# ---------------------------------------------------------------------------

def _noise_analysis(img: np.ndarray, block_size: int = 64) -> dict:
    """
    Estimate local noise variance using Laplacian. Regions with significantly
    different noise levels may indicate splicing or re-saved edits.
    """
    gray = _gray(img).astype(np.float32)

    # Laplacian highlights high-frequency content (noise)
    laplacian = cv2.Laplacian(gray, cv2.CV_64F)
    lap_abs = np.abs(laplacian)

    h, w = gray.shape
    blocks_y = max(h // block_size, 1)
    blocks_x = max(w // block_size, 1)

    noise_map = np.zeros((blocks_y, blocks_x), dtype=np.float32)
    for by in range(blocks_y):
        for bx in range(blocks_x):
            y0 = by * block_size
            y1 = min(y0 + block_size, h)
            x0 = bx * block_size
            x1 = min(x0 + block_size, w)
            block = lap_abs[y0:y1, x0:x1]
            noise_map[by, bx] = float(block.mean()) if block.size > 0 else 0.0

    # Global statistics
    global_mean = float(noise_map.mean())
    global_std = float(noise_map.std()) if noise_map.size > 1 else 0.0

    # Find blocks with anomalous noise (>2 std from mean)
    anomalies = []
    if global_std > 0:
        threshold_high = global_mean + 2 * global_std
        threshold_low = max(global_mean - 2 * global_std, 0)

        for by in range(blocks_y):
            for bx in range(blocks_x):
                val = noise_map[by, bx]
                if val > threshold_high or val < threshold_low:
                    confidence = min(abs(val - global_mean) / (2 * global_std + 1e-6), 1.0)
                    anomalies.append({
                        "x": int(bx * block_size),
                        "y": int(by * block_size),
                        "width": int(min(block_size, w - bx * block_size)),
                        "height": int(min(block_size, h - by * block_size)),
                        "confidence": round(confidence, 4),
                        "description": "Noise inconsistency — potential splicing indicator",
                    })

    anomalies.sort(key=lambda r: r["confidence"], reverse=True)

    return {
        "globalNoiseMean": round(global_mean, 4),
        "globalNoiseStd": round(global_std, 4),
        "regions": anomalies[:10],
    }


# ---------------------------------------------------------------------------
# 3. Edge density analysis
# ---------------------------------------------------------------------------

def _edge_analysis(img: np.ndarray, block_size: int = 64) -> dict:
    """
    Analyse edge density using Canny edge detection. Regions with anomalous
    edge density may indicate copy-paste manipulation or inpainting.
    """
    gray = _gray(img)
    edges = cv2.Canny(gray, 50, 150)

    h, w = edges.shape
    blocks_y = max(h // block_size, 1)
    blocks_x = max(w // block_size, 1)

    density_map = np.zeros((blocks_y, blocks_x), dtype=np.float32)
    for by in range(blocks_y):
        for bx in range(blocks_x):
            y0 = by * block_size
            y1 = min(y0 + block_size, h)
            x0 = bx * block_size
            x1 = min(x0 + block_size, w)
            block = edges[y0:y1, x0:x1]
            density_map[by, bx] = float(np.count_nonzero(block)) / max(block.size, 1)

    global_mean = float(density_map.mean())
    global_std = float(density_map.std()) if density_map.size > 1 else 0.0

    anomalies = []
    if global_std > 0:
        threshold = global_mean + 2 * global_std
        for by in range(blocks_y):
            for bx in range(blocks_x):
                val = density_map[by, bx]
                if val > threshold:
                    confidence = min(abs(val - global_mean) / (2 * global_std + 1e-6), 1.0)
                    anomalies.append({
                        "x": int(bx * block_size),
                        "y": int(by * block_size),
                        "width": int(min(block_size, w - bx * block_size)),
                        "height": int(min(block_size, h - by * block_size)),
                        "confidence": round(confidence, 4),
                        "description": "High edge density — potential copy-paste indicator",
                    })

    anomalies.sort(key=lambda r: r["confidence"], reverse=True)

    return {
        "globalEdgeMean": round(global_mean, 4),
        "globalEdgeStd": round(global_std, 4),
        "regions": anomalies[:10],
    }


# ---------------------------------------------------------------------------
# Finding aggregation and risk scoring
# ---------------------------------------------------------------------------

_SEVERITY_WEIGHT = {"HIGH": 0.3, "MEDIUM": 0.15, "LOW": 0.05}


def _build_findings(ela: dict, noise: dict, edges: dict) -> list:
    """Aggregate forensic findings into a unified findings list."""
    findings = []

    # ELA findings
    ela_regions = ela.get("regions", [])
    if len(ela_regions) > 0:
        max_conf = max(r["confidence"] for r in ela_regions)
        severity = "HIGH" if max_conf > 0.7 else "MEDIUM" if max_conf > 0.3 else "LOW"
        findings.append({
            "type": "ERROR_LEVEL_ANALYSIS",
            "severity": severity,
            "description": f"ELA detected {len(ela_regions)} region(s) with elevated "
                           f"compression error (max confidence: {max_conf:.2f}). "
                           "These regions may have been edited or re-saved.",
            "confidence": round(max_conf, 4),
        })

    # Noise findings
    noise_regions = noise.get("regions", [])
    if len(noise_regions) > 0:
        max_conf = max(r["confidence"] for r in noise_regions)
        severity = "HIGH" if max_conf > 0.7 else "MEDIUM" if max_conf > 0.3 else "LOW"
        findings.append({
            "type": "NOISE_INCONSISTENCY",
            "severity": severity,
            "description": f"Noise analysis detected {len(noise_regions)} region(s) with "
                           f"anomalous noise levels (max confidence: {max_conf:.2f}). "
                           "May indicate splicing or re-saved content.",
            "confidence": round(max_conf, 4),
        })

    # Edge findings
    edge_regions = edges.get("regions", [])
    if len(edge_regions) > 0:
        max_conf = max(r["confidence"] for r in edge_regions)
        severity = "MEDIUM" if max_conf > 0.5 else "LOW"
        findings.append({
            "type": "EDGE_DENSITY_ANOMALY",
            "severity": severity,
            "description": f"Edge analysis detected {len(edge_regions)} region(s) with "
                           f"unusually high edge density (max confidence: {max_conf:.2f}). "
                           "May indicate copy-paste or inpainting.",
            "confidence": round(max_conf, 4),
        })

    # Low-noise / clean image — no findings
    if not findings:
        findings.append({
            "type": "NO_ANOMALIES_DETECTED",
            "severity": "LOW",
            "description": "No significant forensic anomalies detected. "
                           "This does not guarantee the image is unmodified.",
            "confidence": 0.0,
        })

    return findings


def _compute_risk_score(findings: list) -> float:
    """Compute a normalised risk score (0.0–1.0) from findings."""
    total = sum(_SEVERITY_WEIGHT.get(f.get("severity", "LOW"), 0.05) for f in findings
                 if f.get("type") != "NO_ANOMALIES_DETECTED")
    return round(min(total, 1.0), 4)


def _merge_regions(ela: dict, noise: dict, edges: dict) -> list:
    """Merge suspicious regions from all techniques, deduplicated by type."""
    regions = []
    for source, data in [("FORENSICS_ELA", ela), ("FORENSICS_NOISE", noise), ("FORENSICS_EDGE", edges)]:
        for r in data.get("regions", []):
            regions.append({**r, "source": source})
    return regions[:20]  # Cap total regions


# ---------------------------------------------------------------------------
# Public entry point
# ---------------------------------------------------------------------------

def run(file_bytes: bytes, mime_type: str) -> dict:
    """
    Run image forensics analysis on the given file.

    Args:
        file_bytes: Raw file content.
        mime_type:  MIME type string (image/jpeg, image/png, application/pdf).

    Returns:
        Forensics result dict matching the VeriLens forensics module schema.
    """
    start = time.time()

    if not is_available():
        return {
            "status": "FAILED",
            "error": "OpenCV is not available",
            "riskScore": 0.0,
            "findings": [],
            "suspiciousRegions": [],
            "processingTimeMs": int((time.time() - start) * 1000),
        }

    img = _to_cv2_image(file_bytes, mime_type)
    if img is None:
        return {
            "status": "FAILED",
            "error": "Could not decode image for forensics analysis",
            "riskScore": 0.0,
            "findings": [],
            "suspiciousRegions": [],
            "processingTimeMs": int((time.time() - start) * 1000),
        }

    try:
        ela = _error_level_analysis(img)
        noise = _noise_analysis(img)
        edges = _edge_analysis(img)

        findings = _build_findings(ela, noise, edges)
        risk_score = _compute_risk_score(findings)
        regions = _merge_regions(ela, noise, edges)

        return {
            "status": "SUCCESS",
            "riskScore": risk_score,
            "findings": findings,
            "suspiciousRegions": regions,
            "elaMeanError": ela["meanError"],
            "noiseMean": noise["globalNoiseMean"],
            "noiseStd": noise["globalNoiseStd"],
            "edgeMean": edges["globalEdgeMean"],
            "edgeStd": edges["globalEdgeStd"],
            "processingTimeMs": int((time.time() - start) * 1000),
        }

    except Exception as exc:
        return {
            "status": "FAILED",
            "error": f"Forensics analysis error: {exc}",
            "riskScore": 0.0,
            "findings": [],
            "suspiciousRegions": [],
            "processingTimeMs": int((time.time() - start) * 1000),
        }
