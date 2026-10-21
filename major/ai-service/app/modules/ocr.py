"""
OCR Module — Phase 4.

Uses pytesseract (Tesseract) to extract text from document images and PDFs.
Returns structured evidence following the VeriLens module schema.

OCR output is evidence, not ground truth.
Confidence values are preserved and surfaced.
"""

import io
import time
from typing import Optional

from PIL import Image

# Guard imports so the module can be loaded even if Tesseract is not installed.
# The health endpoint will report UNAVAILABLE in that case.
try:
    import pytesseract
    TESSERACT_AVAILABLE = True
except ImportError:
    TESSERACT_AVAILABLE = False

try:
    from pdf2image import convert_from_bytes
    PDF2IMAGE_AVAILABLE = True
except ImportError:
    PDF2IMAGE_AVAILABLE = False


def is_available() -> bool:
    """Return True if Tesseract OCR is installed and usable."""
    if not TESSERACT_AVAILABLE:
        return False
    try:
        pytesseract.get_tesseract_version()
        return True
    except Exception:
        return False


def _image_from_bytes(file_bytes: bytes, mime_type: str) -> list:
    """
    Convert uploaded file bytes to a list of PIL Images.
    PDFs are converted page-by-page; images return as a single-element list.
    Returns empty list on failure.
    """
    images = []
    try:
        if mime_type == "application/pdf":
            if not PDF2IMAGE_AVAILABLE:
                return []
            pages = convert_from_bytes(file_bytes, dpi=200)
            images = pages[:5]  # Cap at 5 pages to bound runtime
        else:
            img = Image.open(io.BytesIO(file_bytes)).convert("RGB")
            images = [img]
    except Exception:
        pass
    return images


def _extract_with_data(image: Image.Image) -> dict:
    """
    Run pytesseract with detailed output (bounding boxes + confidence per word).
    Returns dict with keys: text, words.
    """
    raw = pytesseract.image_to_string(image, lang="eng")

    detail = pytesseract.image_to_data(
        image, lang="eng", output_type=pytesseract.Output.DICT
    )

    words = []
    for i, word_text in enumerate(detail["text"]):
        word_text = word_text.strip()
        if not word_text:
            continue
        conf = int(detail["conf"][i])
        if conf < 0:
            continue  # pytesseract uses -1 for non-word rows
        words.append({
            "text": word_text,
            "confidence": round(conf / 100.0, 4),
            "bbox": [
                detail["left"][i],
                detail["top"][i],
                detail["left"][i] + detail["width"][i],
                detail["top"][i] + detail["height"][i],
            ],
        })

    return {"text": raw.strip(), "words": words}


def _overall_confidence(words: list) -> float:
    """Compute mean confidence across all extracted words."""
    if not words:
        return 0.0
    return round(sum(w["confidence"] for w in words) / len(words), 4)


def run(file_bytes: bytes, mime_type: str) -> dict:
    """
    Run OCR on the given file bytes.

    Args:
        file_bytes: Raw file content.
        mime_type:  MIME type string (application/pdf, image/jpeg, image/png).

    Returns:
        OCR result dict matching the VeriLens OCR module schema.
    """
    start = time.time()

    if not is_available():
        return {
            "status": "FAILED",
            "error": "Tesseract OCR is not available on this system",
            "extractedText": None,
            "words": [],
            "overallConfidence": 0.0,
            "pageCount": 0,
            "processingTimeMs": int((time.time() - start) * 1000),
        }

    images = _image_from_bytes(file_bytes, mime_type)
    if not images:
        return {
            "status": "FAILED",
            "error": "Could not decode document image",
            "extractedText": None,
            "words": [],
            "overallConfidence": 0.0,
            "pageCount": 0,
            "processingTimeMs": int((time.time() - start) * 1000),
        }

    all_text_parts = []
    all_words = []

    for img in images:
        try:
            result = _extract_with_data(img)
            if result["text"]:
                all_text_parts.append(result["text"])
            all_words.extend(result["words"])
        except Exception as exc:
            # One page failing should not abort the whole document
            all_text_parts.append(f"[Page extraction failed: {exc}]")

    full_text = "\n\n".join(all_text_parts).strip()
    overall_conf = _overall_confidence(all_words)

    return {
        "status": "SUCCESS",
        "extractedText": full_text if full_text else None,
        "words": all_words,
        "overallConfidence": overall_conf,
        "pageCount": len(images),
        "processingTimeMs": int((time.time() - start) * 1000),
    }
