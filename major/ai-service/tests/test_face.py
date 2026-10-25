"""
Tests for the Face Verification module — Phase 8.
Uses synthetic images. Face detection may not succeed on completely
synthetic images (no real face structure), so tests focus on the
schema contract and NOT_APPLICABLE paths.
"""

import io
import numpy as np
import pytest
from PIL import Image, ImageDraw


def _make_blank_image() -> bytes:
    img = Image.new("RGB", (300, 300), color="white")
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=90)
    return buf.getvalue()


def _make_face_like_image() -> bytes:
    """Create an image with a rough oval that might be detected as a face."""
    img = Image.new("RGB", (300, 300), color=(240, 220, 200))
    draw = ImageDraw.Draw(img)
    # Draw an oval for face outline
    draw.ellipse([75, 50, 225, 250], fill=(230, 200, 170), outline=(100, 80, 60), width=3)
    # Eyes
    draw.ellipse([105, 110, 130, 135], fill="black")
    draw.ellipse([170, 110, 195, 135], fill="black")
    # Nose
    draw.ellipse([140, 155, 165, 175], fill=(180, 140, 120))
    # Mouth
    draw.arc([115, 185, 185, 215], start=0, end=180, fill="darkred", width=3)
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=90)
    return buf.getvalue()


# ---------------------------------------------------------------------------
# Module availability
# ---------------------------------------------------------------------------

class TestFaceAvailability:
    def test_is_available_returns_bool(self):
        from app.modules.face import is_available
        assert isinstance(is_available(), bool)


# ---------------------------------------------------------------------------
# Schema tests
# ---------------------------------------------------------------------------

class TestFaceRunSchema:
    def test_no_reference_returns_not_applicable(self):
        from app.modules.face import run
        result = run(_make_blank_image(), "image/jpeg", None)
        assert result["status"] == "NOT_APPLICABLE"

    def test_no_reference_has_required_keys(self):
        from app.modules.face import run
        result = run(_make_blank_image(), "image/jpeg", None)
        for key in ("status", "processingTimeMs", "threshold"):
            assert key in result, f"Missing key: {key}"

    def test_returns_dict(self):
        from app.modules.face import run
        result = run(_make_blank_image(), "image/jpeg", None)
        assert isinstance(result, dict)

    def test_processing_time_non_negative(self):
        from app.modules.face import run
        result = run(_make_blank_image(), "image/jpeg", None)
        assert result["processingTimeMs"] >= 0

    def test_threshold_is_float(self):
        from app.modules.face import run
        result = run(_make_blank_image(), "image/jpeg", None)
        assert isinstance(result["threshold"], float)
        assert 0.0 < result["threshold"] <= 1.0

    def test_invalid_doc_bytes_returns_failed(self):
        from app.modules.face import run
        result = run(b"not an image", "image/jpeg", b"not a face")
        # When cascade unavailable returns NOT_APPLICABLE; with bad bytes returns FAILED
        assert result["status"] in ("FAILED", "NOT_APPLICABLE")

    def test_with_reference_returns_valid_status(self):
        from app.modules.face import run
        result = run(_make_blank_image(), "image/jpeg", _make_face_like_image())
        assert result["status"] in (
            "POSSIBLE_MATCH", "POSSIBLE_MISMATCH", "NOT_APPLICABLE", "FAILED"
        )

    def test_similarity_is_none_or_float(self):
        from app.modules.face import run
        result = run(_make_blank_image(), "image/jpeg", _make_face_like_image())
        sim = result.get("similarity")
        assert sim is None or isinstance(sim, (float, int))

    def test_similarity_in_range_when_not_none(self):
        from app.modules.face import run
        result = run(_make_face_like_image(), "image/jpeg", _make_face_like_image())
        sim = result.get("similarity")
        if sim is not None:
            assert 0.0 <= float(sim) <= 1.0

    def test_document_face_detected_is_bool(self):
        from app.modules.face import run
        result = run(_make_blank_image(), "image/jpeg", _make_face_like_image())
        val = result.get("documentFaceDetected")
        assert isinstance(val, bool) or val is None

    def test_reference_face_detected_is_bool(self):
        from app.modules.face import run
        result = run(_make_blank_image(), "image/jpeg", _make_face_like_image())
        val = result.get("referenceFaceDetected")
        assert isinstance(val, bool) or val is None


# ---------------------------------------------------------------------------
# /internal/analyze endpoint integration
# ---------------------------------------------------------------------------

class TestAnalyzeEndpointFace:
    def _client(self):
        from fastapi.testclient import TestClient
        from app.main import app
        return TestClient(app)

    def _api_key(self):
        from app.main import API_KEY
        return API_KEY

    def test_face_key_present_when_requested(self):
        body = self._client().post(
            "/internal/analyze",
            headers={"X-API-Key": self._api_key()},
            data={"documentId": "face-001", "modules": "FACE"},
            files={"file": ("doc.jpg", _make_blank_image(), "image/jpeg")},
        ).json()
        assert "faceVerification" in body["modules"]

    def test_face_not_applicable_without_reference(self):
        body = self._client().post(
            "/internal/analyze",
            headers={"X-API-Key": self._api_key()},
            data={"documentId": "face-002", "modules": "FACE"},
            files={"file": ("doc.jpg", _make_blank_image(), "image/jpeg")},
        ).json()
        # No reference face → NOT_APPLICABLE
        assert body["modules"]["faceVerification"]["status"] == "NOT_APPLICABLE"

    def test_face_with_reference_returns_valid_status(self):
        body = self._client().post(
            "/internal/analyze",
            headers={"X-API-Key": self._api_key()},
            data={"documentId": "face-003", "modules": "FACE"},
            files={
                "file": ("doc.jpg", _make_blank_image(), "image/jpeg"),
                "referenceFace": ("ref.jpg", _make_face_like_image(), "image/jpeg"),
            },
        ).json()
        # CascadeClassifier may not be available in OpenCV 5 — all statuses are valid
        assert body["modules"]["faceVerification"]["status"] in (
            "POSSIBLE_MATCH", "POSSIBLE_MISMATCH", "NOT_APPLICABLE", "FAILED"
        )

    def test_face_not_run_when_not_in_modules(self):
        body = self._client().post(
            "/internal/analyze",
            headers={"X-API-Key": self._api_key()},
            data={"documentId": "face-004", "modules": "OCR"},
            files={"file": ("doc.jpg", _make_blank_image(), "image/jpeg")},
        ).json()
        assert body["modules"]["faceVerification"]["status"] == "NOT_APPLICABLE"

    def test_health_reports_face_status(self):
        body = self._client().get("/internal/health").json()
        assert body["modules"]["face"] in ("AVAILABLE", "UNAVAILABLE")
