"""
Metadata Analysis Module — Phase 5.

Extracts and analyses file-level metadata from documents (JPEG, PNG, PDF).
Returns structured evidence following the VeriLens module schema.

Important: Metadata is supporting evidence, not proof of tampering.
- Metadata can be missing or deliberately stripped — absence ≠ tampering.
- Metadata can be spoofed — presence ≠ authenticity.
- Findings are labelled as "potential indicators" only.
"""

import io
import os
import time
from datetime import datetime
from typing import Optional

from PIL import Image, ExifTags

try:
    import fitz  # PyMuPDF
    PYMUPDF_AVAILABLE = True
except ImportError:
    PYMUPDF_AVAILABLE = False


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _safe_str(value) -> Optional[str]:
    """Convert a value to str, returning None if empty/None."""
    if value is None:
        return None
    s = str(value).strip()
    return s if s else None


def _decode_exif(raw_exif: dict) -> dict:
    """Convert numeric EXIF tag IDs to human-readable names."""
    decoded = {}
    for tag_id, value in raw_exif.items():
        tag_name = ExifTags.TAGS.get(tag_id, str(tag_id))
        # Skip binary data fields
        if isinstance(value, bytes):
            decoded[tag_name] = "<binary>"
        elif isinstance(value, tuple):
            decoded[tag_name] = list(value)
        else:
            decoded[tag_name] = value
    return decoded


def _anomaly(atype: str, severity: str, description: str) -> dict:
    return {"type": atype, "severity": severity, "description": description}


# ---------------------------------------------------------------------------
# Image metadata extraction
# ---------------------------------------------------------------------------

def _extract_image_metadata(file_bytes: bytes, mime_type: str) -> dict:
    """Extract metadata from JPEG/PNG using Pillow."""
    properties = {}
    anomalies = []

    try:
        img = Image.open(io.BytesIO(file_bytes))
        properties["fileType"] = img.format or mime_type
        properties["mode"] = img.mode
        properties["dimensions"] = {"width": img.width, "height": img.height}

        # EXIF data (primarily JPEG)
        raw_exif = {}
        try:
            raw_exif = img._getexif() or {}
        except (AttributeError, Exception):
            pass

        if raw_exif:
            exif = _decode_exif(raw_exif)
            properties["exif"] = exif

            # Software field
            software = _safe_str(exif.get("Software"))
            properties["software"] = software

            # Timestamps
            ts = {}
            for tag in ("DateTime", "DateTimeOriginal", "DateTimeDigitized"):
                val = _safe_str(exif.get(tag))
                if val:
                    ts[tag] = val
            if ts:
                properties["timestamps"] = ts

            # --- Anomaly checks ---

            # Known editing software signatures
            editing_software = [
                "adobe photoshop", "gimp", "paint.net", "inkscape",
                "affinity", "pixelmator", "canva", "lightroom",
                "capture one", "darktable",
            ]
            if software:
                sw_lower = software.lower()
                for sig in editing_software:
                    if sig in sw_lower:
                        anomalies.append(_anomaly(
                            "EDITING_SOFTWARE_DETECTED",
                            "MEDIUM",
                            f"Image was processed with editing software: {software}. "
                            "This is a potential indicator — many legitimate documents "
                            "are scanned or exported via editing tools.",
                        ))
                        break

            # DateTime inconsistency: Original should not be after digitized
            dt_original = _safe_str(exif.get("DateTimeOriginal"))
            dt_digitized = _safe_str(exif.get("DateTimeDigitized"))
            dt_modified = _safe_str(exif.get("DateTime"))
            if dt_original and dt_modified:
                try:
                    fmt = "%Y:%m:%d %H:%M:%S"
                    orig = datetime.strptime(dt_original, fmt)
                    mod = datetime.strptime(dt_modified, fmt)
                    if mod < orig:
                        anomalies.append(_anomaly(
                            "TIMESTAMP_INCONSISTENCY",
                            "MEDIUM",
                            f"Modification timestamp ({dt_modified}) is earlier than "
                            f"original capture timestamp ({dt_original}).",
                        ))
                except ValueError:
                    pass

            # GPS data in document (unusual for official documents)
            if "GPSInfo" in exif:
                anomalies.append(_anomaly(
                    "GPS_DATA_PRESENT",
                    "LOW",
                    "GPS coordinates are embedded in the image. "
                    "Unusual for official documents — may indicate a mobile phone photo.",
                ))

            # Thumbnail present but dimensions mismatch main image
            thumbnail = exif.get("ThumbnailImage")
            if thumbnail:
                try:
                    thumb_img = Image.open(io.BytesIO(thumbnail if isinstance(thumbnail, bytes) else b""))
                    # If thumbnail dimensions are unusually large relative to main, flag
                    if thumb_img.width > img.width or thumb_img.height > img.height:
                        anomalies.append(_anomaly(
                            "THUMBNAIL_LARGER_THAN_IMAGE",
                            "MEDIUM",
                            "Embedded thumbnail is larger than the main image — "
                            "possible indicator of cropping or resizing.",
                        ))
                except Exception:
                    pass

        else:
            properties["exif"] = {}
            properties["software"] = None
            properties["timestamps"] = {}
            # JPEG without EXIF is notable
            if mime_type == "image/jpeg":
                anomalies.append(_anomaly(
                    "NO_EXIF_DATA",
                    "LOW",
                    "JPEG has no EXIF metadata. Could indicate metadata stripping.",
                ))

        # DPI / resolution check
        try:
            dpi = img.info.get("dpi") or img.info.get("resolution")
            if dpi:
                dpi_x = dpi[0] if isinstance(dpi, tuple) else dpi
                properties["dpi"] = float(dpi_x)
                if float(dpi_x) < 72:
                    anomalies.append(_anomaly(
                        "LOW_RESOLUTION",
                        "LOW",
                        f"Image resolution is very low ({dpi_x} DPI). "
                        "Low resolution may indicate a screenshot or heavily compressed image.",
                    ))
        except Exception:
            pass

    except Exception as exc:
        return {
            "status": "FAILED",
            "error": f"Failed to extract image metadata: {exc}",
            "available": False,
            "properties": {},
            "anomalies": [],
            "evidenceScore": 0.0,
        }

    return {
        "status": "SUCCESS",
        "available": True,
        "properties": properties,
        "anomalies": anomalies,
    }


# ---------------------------------------------------------------------------
# PDF metadata extraction
# ---------------------------------------------------------------------------

def _extract_pdf_metadata(file_bytes: bytes) -> dict:
    """Extract metadata from PDF using PyMuPDF."""
    if not PYMUPDF_AVAILABLE:
        return {
            "status": "FAILED",
            "error": "PyMuPDF not available",
            "available": False,
            "properties": {},
            "anomalies": [],
            "evidenceScore": 0.0,
        }

    anomalies = []
    properties: dict = {"fileType": "PDF"}

    try:
        doc = fitz.open(stream=file_bytes, filetype="pdf")
        meta = doc.metadata or {}

        properties["pageCount"] = doc.page_count
        properties["isEncrypted"] = doc.is_encrypted

        # Standard PDF metadata fields
        author = _safe_str(meta.get("author"))
        creator = _safe_str(meta.get("creator"))
        producer = _safe_str(meta.get("producer"))
        subject = _safe_str(meta.get("subject"))
        title = _safe_str(meta.get("title"))
        creation_date = _safe_str(meta.get("creationDate"))
        mod_date = _safe_str(meta.get("modDate"))

        properties["author"] = author
        properties["title"] = title
        properties["subject"] = subject
        properties["creator"] = creator
        properties["producer"] = producer
        properties["timestamps"] = {
            k: v for k, v in {
                "creationDate": creation_date,
                "modDate": mod_date,
            }.items() if v
        }

        # Software field synthesised from creator + producer
        software_parts = [p for p in [creator, producer] if p]
        properties["software"] = "; ".join(software_parts) if software_parts else None

        # --- Anomaly checks ---

        editing_tools = [
            "adobe photoshop", "gimp", "paint.net", "inkscape",
            "affinity", "pixelmator", "canva", "microsoft word",
            "libreoffice writer", "openoffice",
        ]
        combined = " ".join(software_parts).lower()
        for sig in editing_tools:
            if sig in combined:
                anomalies.append(_anomaly(
                    "EDITING_SOFTWARE_IN_PDF",
                    "MEDIUM",
                    f"PDF was created or modified using: {'; '.join(software_parts)}. "
                    "This is a potential indicator — official documents are typically "
                    "produced by specialised issuance software.",
                ))
                break

        # Creation date after modification date
        if creation_date and mod_date and mod_date < creation_date:
            anomalies.append(_anomaly(
                "PDF_TIMESTAMP_INCONSISTENCY",
                "MEDIUM",
                f"PDF modification date ({mod_date}) is earlier than "
                f"creation date ({creation_date}).",
            ))

        # No metadata at all is suspicious for an official document
        if not any([author, creator, producer, creation_date]):
            anomalies.append(_anomaly(
                "NO_PDF_METADATA",
                "LOW",
                "PDF has no author, creator, producer, or creation date metadata. "
                "Metadata stripping is sometimes used to obscure document origin.",
            ))

        # Very high page count vs expected (most identity docs are 1–4 pages)
        if doc.page_count > 20:
            anomalies.append(_anomaly(
                "UNEXPECTED_PAGE_COUNT",
                "LOW",
                f"PDF has {doc.page_count} pages, which is unusual for an identity document.",
            ))

        doc.close()

    except Exception as exc:
        return {
            "status": "FAILED",
            "error": f"Failed to extract PDF metadata: {exc}",
            "available": False,
            "properties": properties,
            "anomalies": anomalies,
            "evidenceScore": 0.0,
        }

    return {
        "status": "SUCCESS",
        "available": True,
        "properties": properties,
        "anomalies": anomalies,
    }


# ---------------------------------------------------------------------------
# Evidence score
# ---------------------------------------------------------------------------

_SEVERITY_WEIGHT = {"HIGH": 0.4, "MEDIUM": 0.2, "LOW": 0.05}


def _compute_evidence_score(anomalies: list) -> float:
    """
    Compute a normalised evidence score (0.0–1.0) from detected anomalies.
    More / higher-severity anomalies produce a higher score.
    Score is capped at 1.0.
    """
    if not anomalies:
        return 0.0
    total = sum(_SEVERITY_WEIGHT.get(a.get("severity", "LOW"), 0.05) for a in anomalies)
    return round(min(total, 1.0), 4)


# ---------------------------------------------------------------------------
# Public entry point
# ---------------------------------------------------------------------------

def run(file_bytes: bytes, mime_type: str) -> dict:
    """
    Run metadata analysis on the given file.

    Args:
        file_bytes: Raw file content.
        mime_type:  MIME type string.

    Returns:
        Metadata result dict matching the VeriLens metadata module schema.
    """
    start = time.time()

    try:
        if mime_type == "application/pdf":
            result = _extract_pdf_metadata(file_bytes)
        elif mime_type in ("image/jpeg", "image/png", "image/jpg"):
            result = _extract_image_metadata(file_bytes, mime_type)
        else:
            # Unsupported format — not a failure, just not applicable
            return {
                "status": "NOT_APPLICABLE",
                "available": False,
                "properties": {},
                "anomalies": [],
                "evidenceScore": 0.0,
                "processingTimeMs": int((time.time() - start) * 1000),
            }

        if result.get("status") != "SUCCESS":
            result["processingTimeMs"] = int((time.time() - start) * 1000)
            return result

        anomalies = result.get("anomalies", [])
        result["evidenceScore"] = _compute_evidence_score(anomalies)
        result["processingTimeMs"] = int((time.time() - start) * 1000)
        return result

    except Exception as exc:
        return {
            "status": "FAILED",
            "error": f"Unexpected error in metadata analysis: {exc}",
            "available": False,
            "properties": {},
            "anomalies": [],
            "evidenceScore": 0.0,
            "processingTimeMs": int((time.time() - start) * 1000),
        }
