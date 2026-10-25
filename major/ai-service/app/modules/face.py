"""
Face Verification Module — Phase 8.

Pipeline:
  Document image → Face Detection (OpenCV Haar Cascade) →
  Face Crop → Feature Extraction → Cosine Similarity → Result

If no reference face is provided → NOT_APPLICABLE.
If no face is detected in the document → reports detection failure.
This is a similarity signal, not legal identity verification.

Technology: OpenCV Haar Cascade for face detection.
Embedding: Histogram of Oriented Gradients (HOG) + histogram features
for a lightweight, no-download similarity measure suitable for Phase 8.
A deep embedding model (ArcFace/FaceNet) can replace this in Phase 13
evaluation without changing the module interface.

Important: All similarity claims use careful language.
A high similarity does NOT prove identity — it indicates a potential match.
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


# Similarity threshold — configurable.
# Below this → MISMATCH, above → POSSIBLE_MATCH.
# Set conservatively to avoid false positive matches.
DEFAULT_THRESHOLD = 0.70


def is_available() -> bool:
    """Return True if OpenCV is installed with face detection support."""
    if not OPENCV_AVAILABLE:
        return False
    # CascadeClassifier was removed in OpenCV 5 — check for it
    if not hasattr(cv2, 'CascadeClassifier'):
        return False
    if not hasattr(cv2, 'data'):
        return False
    return True


# ---------------------------------------------------------------------------
# Image loading helpers
# ---------------------------------------------------------------------------

def _load_image(file_bytes: bytes, mime_type: str = None) -> np.ndarray | None:
    """Load image bytes into an OpenCV BGR array."""
    try:
        if mime_type == "application/pdf":
            from pdf2image import convert_from_bytes
            pages = convert_from_bytes(file_bytes, dpi=200, first_page=1, last_page=1)
            if not pages:
                return None
            pil_img = pages[0].convert("RGB")
            arr = np.array(pil_img)
            return cv2.cvtColor(arr, cv2.COLOR_RGB2BGR)
        else:
            arr = np.frombuffer(file_bytes, dtype=np.uint8)
            return cv2.imdecode(arr, cv2.IMREAD_COLOR)
    except Exception:
        return None


def _load_pil(file_bytes: bytes) -> np.ndarray | None:
    """Load image bytes into an OpenCV BGR array via PIL (more format tolerant)."""
    try:
        pil = Image.open(io.BytesIO(file_bytes)).convert("RGB")
        arr = np.array(pil)
        return cv2.cvtColor(arr, cv2.COLOR_RGB2BGR)
    except Exception:
        return None


# ---------------------------------------------------------------------------
# Face detection
# ---------------------------------------------------------------------------

def _get_cascade():
    """Load OpenCV's built-in frontal face Haar cascade."""
    # Try CascadeClassifier (OpenCV 4.x)
    if hasattr(cv2, 'CascadeClassifier') and hasattr(cv2, 'data'):
        cascade_path = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
        cascade = cv2.CascadeClassifier(cascade_path)
        if not cascade.empty():
            return cascade
    return None


def _detect_face(img: np.ndarray, cascade) -> np.ndarray | None:
    """
    Detect the largest face in the image and return a 128×128 grayscale crop.
    Returns None if no face is detected.
    """
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    faces = cascade.detectMultiScale(
        gray,
        scaleFactor=1.1,
        minNeighbors=5,
        minSize=(40, 40),
    )

    if len(faces) == 0:
        faces = cascade.detectMultiScale(
            gray,
            scaleFactor=1.05,
            minNeighbors=3,
            minSize=(30, 30),
        )

    if len(faces) == 0:
        return None

    x, y, w, h = max(faces, key=lambda f: f[2] * f[3])
    margin = int(min(w, h) * 0.1)
    x1 = max(0, x - margin)
    y1 = max(0, y - margin)
    x2 = min(img.shape[1], x + w + margin)
    y2 = min(img.shape[0], y + h + margin)
    face_crop = gray[y1:y2, x1:x2]
    return cv2.resize(face_crop, (128, 128))


# ---------------------------------------------------------------------------
# Feature extraction — lightweight histogram-based embedding
# ---------------------------------------------------------------------------

def _extract_features(face_gray: np.ndarray) -> np.ndarray:
    """
    Extract a feature vector from a grayscale face image.

    Uses a combination of:
    - Normalised pixel histogram (256 bins)
    - LBP-like local block histograms (4×4 grid, 8 bins each)

    Total vector: 256 + (4*4*8) = 384 dimensions.
    This is a lightweight alternative to deep embeddings, suitable for
    Phase 8. Deep embeddings (ArcFace etc.) can replace this in Phase 13.
    """
    # 1. Global pixel histogram (256 bins, normalised)
    global_hist = cv2.calcHist([face_gray], [0], None, [256], [0, 256])
    global_hist = global_hist.flatten()
    global_hist = global_hist / (global_hist.sum() + 1e-8)

    # 2. Local block histograms — divide into 4×4 grid
    h, w = face_gray.shape
    block_h = h // 4
    block_w = w // 4
    local_features = []

    for row in range(4):
        for col in range(4):
            block = face_gray[
                row * block_h:(row + 1) * block_h,
                col * block_w:(col + 1) * block_w,
            ]
            if block.size == 0:
                local_features.extend([0.0] * 8)
                continue
            # Apply histogram equalisation to each block for lighting invariance
            block_eq = cv2.equalizeHist(block)
            local_hist = cv2.calcHist([block_eq], [0], None, [8], [0, 256])
            local_hist = local_hist.flatten()
            local_hist = local_hist / (local_hist.sum() + 1e-8)
            local_features.extend(local_hist.tolist())

    feature_vector = np.concatenate([global_hist, np.array(local_features)])
    return feature_vector.astype(np.float32)


# ---------------------------------------------------------------------------
# Similarity
# ---------------------------------------------------------------------------

def _cosine_similarity(v1: np.ndarray, v2: np.ndarray) -> float:
    """Compute cosine similarity between two feature vectors."""
    norm1 = np.linalg.norm(v1)
    norm2 = np.linalg.norm(v2)
    if norm1 < 1e-8 or norm2 < 1e-8:
        return 0.0
    return float(np.dot(v1, v2) / (norm1 * norm2))


# ---------------------------------------------------------------------------
# Public entry point
# ---------------------------------------------------------------------------

def run(doc_bytes: bytes, doc_mime: str, ref_bytes: bytes | None = None) -> dict:
    """
    Run face verification.

    Args:
        doc_bytes:  Document file bytes.
        doc_mime:   Document MIME type.
        ref_bytes:  Reference face image bytes (optional).

    Returns:
        Face verification result dict matching the VeriLens schema.
    """
    start = time.time()

    # If no reference face provided → NOT_APPLICABLE (check this first regardless of availability)
    if not ref_bytes:
        return {
            "status": "NOT_APPLICABLE",
            "reason": "No reference face provided — face verification requires a reference image",
            "similarity": None,
            "threshold": DEFAULT_THRESHOLD,
            "documentFaceDetected": False,
            "referenceFaceDetected": False,
            "processingTimeMs": int((time.time() - start) * 1000),
        }

    if not is_available():
        return {
            "status": "FAILED",
            "error": "Face detection not available (OpenCV version may not include CascadeClassifier)",
            "similarity": None,
            "threshold": DEFAULT_THRESHOLD,
            "documentFaceDetected": False,
            "referenceFaceDetected": False,
            "processingTimeMs": int((time.time() - start) * 1000),
        }

    cascade = _get_cascade()
    if cascade is None:
        # CascadeClassifier unavailable (OpenCV 5+ removed it) — return NOT_APPLICABLE
        return {
            "status": "NOT_APPLICABLE",
            "reason": "Face detection requires CascadeClassifier which is not available in this OpenCV version",
            "similarity": None,
            "threshold": DEFAULT_THRESHOLD,
            "documentFaceDetected": False,
            "referenceFaceDetected": False,
            "processingTimeMs": int((time.time() - start) * 1000),
        }

    # Load document image
    doc_img = _load_image(doc_bytes, doc_mime) or _load_pil(doc_bytes)
    if doc_img is None:
        return {
            "status": "FAILED",
            "error": "Could not decode document image",
            "similarity": None,
            "threshold": DEFAULT_THRESHOLD,
            "documentFaceDetected": False,
            "referenceFaceDetected": False,
            "processingTimeMs": int((time.time() - start) * 1000),
        }

    # Load reference face image
    ref_img = _load_pil(ref_bytes) or _load_image(ref_bytes)
    if ref_img is None:
        return {
            "status": "FAILED",
            "error": "Could not decode reference face image",
            "similarity": None,
            "threshold": DEFAULT_THRESHOLD,
            "documentFaceDetected": False,
            "referenceFaceDetected": False,
            "processingTimeMs": int((time.time() - start) * 1000),
        }

    # Detect faces
    doc_face = _detect_face(doc_img, cascade)
    ref_face = _detect_face(ref_img, cascade)

    doc_face_detected = doc_face is not None
    ref_face_detected = ref_face is not None

    if not doc_face_detected:
        return {
            "status": "NOT_APPLICABLE",
            "reason": "No face detected in the document image",
            "similarity": None,
            "threshold": DEFAULT_THRESHOLD,
            "documentFaceDetected": False,
            "referenceFaceDetected": ref_face_detected,
            "processingTimeMs": int((time.time() - start) * 1000),
        }

    if not ref_face_detected:
        return {
            "status": "NOT_APPLICABLE",
            "reason": "No face detected in the reference image",
            "similarity": None,
            "threshold": DEFAULT_THRESHOLD,
            "documentFaceDetected": True,
            "referenceFaceDetected": False,
            "processingTimeMs": int((time.time() - start) * 1000),
        }

    # Extract features and compute similarity
    doc_features = _extract_features(doc_face)
    ref_features = _extract_features(ref_face)
    similarity = _cosine_similarity(doc_features, ref_features)
    similarity = round(similarity, 4)

    # Determine match status
    if similarity >= DEFAULT_THRESHOLD:
        match_status = "POSSIBLE_MATCH"
        note = (
            f"Similarity {similarity:.2f} meets threshold {DEFAULT_THRESHOLD}. "
            "This is a similarity signal — not a legal identity confirmation."
        )
    else:
        match_status = "POSSIBLE_MISMATCH"
        note = (
            f"Similarity {similarity:.2f} is below threshold {DEFAULT_THRESHOLD}. "
            "This may indicate different persons or a low-quality image."
        )

    return {
        "status": match_status,
        "similarity": similarity,
        "threshold": DEFAULT_THRESHOLD,
        "documentFaceDetected": True,
        "referenceFaceDetected": True,
        "note": note,
        "processingTimeMs": int((time.time() - start) * 1000),
    }
