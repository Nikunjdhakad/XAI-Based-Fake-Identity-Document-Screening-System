"""
Tests for the OCR module — Phase 4.

Uses synthetic PIL images rather than real documents.
Tests run without requiring the Tesseract binary on the host machine
(Tesseract-dependent assertions are skipped via pytest.mark.skipif).
"""

import io
import pytest
from PIL import Image, ImageDraw


def _make_test_image(text: str = "TEST DOCUMENT\nName: John Doe\nDate: 2024-01-15") -> bytes:
    """Create a minimal PIL image with printed text for fixture testing."""
    img = Image.new("RGB", (500, 200), color="white")
    draw = ImageDraw.Draw(img)
    draw.text((20, 20), text, fill="black")
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


# ---------------------------------------------------------------------------
# Module availability
# ---------------------------------------------------------------------------

class TestOcrAvailability:
    def test_is_available_returns_bool(self):
        from app.modules.ocr import is_available
        assert isinstance(is_available(), bool)


# ---------------------------------------------------------------------------
# run() output schema
# ---------------------------------------------------------------------------

class TestOcrRunSchema:
    def test_returns_dict(self):
        from app.modules.ocr import run
        result = run(_make_test_image(), "image/png")
        assert isinstance(result, dict)

    def test_has_required_keys(self):
        from app.modules.ocr import run
        result = run(_make_test_image(), "image/png")
        for key in ("status", "processingTimeMs", "words", "overallConfidence", "pageCount"):
            assert key in result, f"Missing key: {key}"

    def test_status_is_valid_value(self):
        from app.modules.ocr import run
        result = run(_make_test_image(), "image/png")
        assert result["status"] in ("SUCCESS", "FAILED", "NOT_APPLICABLE")

    def test_processing_time_non_negative(self):
        from app.modules.ocr import run
        result = run(_make_test_image(), "image/png")
        assert result["processingTimeMs"] >= 0

    def test_words_is_list(self):
        from app.modules.ocr import run
        result = run(_make_test_image(), "image/png")
        assert isinstance(result["words"], list)

    def test_invalid_bytes_returns_failed(self):
        from app.modules.ocr import run
        result = run(b"not an image", "image/jpeg")
        assert result["status"] == "FAILED"

    def test_confidence_between_0_and_1_when_success(self):
        from app.modules.ocr import run, is_available
        if not is_available():
            pytest.skip("Tesseract not installed — skipping confidence assertion")
        result = run(_make_test_image(), "image/png")
        if result["status"] == "SUCCESS":
            assert 0.0 <= result["overallConfidence"] <= 1.0

    def test_page_count_is_int(self):
        from app.modules.ocr import run
        result = run(_make_test_image(), "image/png")
        assert isinstance(result["pageCount"], int)


# ---------------------------------------------------------------------------
# FastAPI /internal/analyze endpoint
# ---------------------------------------------------------------------------

class TestAnalyzeEndpoint:
    def _client(self):
        from fastapi.testclient import TestClient
        from app.main import app
        return TestClient(app)

    def _api_key(self):
        from app.main import API_KEY
        return API_KEY

    def test_analyze_returns_200_with_valid_image(self):
        response = self._client().post(
            "/internal/analyze",
            headers={"X-API-Key": self._api_key()},
            data={"documentId": "test-001", "modules": "OCR"},
            files={"file": ("doc.png", _make_test_image(), "image/png")},
        )
        assert response.status_code == 200

    def test_analyze_response_has_required_fields(self):
        body = self._client().post(
            "/internal/analyze",
            headers={"X-API-Key": self._api_key()},
            data={"documentId": "test-002", "modules": "OCR"},
            files={"file": ("doc.png", _make_test_image(), "image/png")},
        ).json()
        for key in ("documentId", "status", "modules", "processingTimeMs"):
            assert key in body, f"Missing field: {key}"

    def test_analyze_ocr_key_present_in_modules(self):
        body = self._client().post(
            "/internal/analyze",
            headers={"X-API-Key": self._api_key()},
            data={"documentId": "test-003", "modules": "OCR"},
            files={"file": ("doc.png", _make_test_image(), "image/png")},
        ).json()
        assert "ocr" in body["modules"]

    def test_non_ocr_modules_are_not_applicable(self):
        body = self._client().post(
            "/internal/analyze",
            headers={"X-API-Key": self._api_key()},
            data={"documentId": "test-004", "modules": "OCR"},
            files={"file": ("doc.png", _make_test_image(), "image/png")},
        ).json()
        for key in ("forensics", "metadata", "identity", "faceVerification", "aiManipulation"):
            assert body["modules"][key]["status"] == "NOT_APPLICABLE", \
                f"Module {key} should be NOT_APPLICABLE, got: {body['modules'][key]}"

    def test_missing_api_key_returns_401(self):
        response = self._client().post(
            "/internal/analyze",
            data={"documentId": "test-005", "modules": "OCR"},
            files={"file": ("doc.png", _make_test_image(), "image/png")},
        )
        assert response.status_code == 401

    def test_health_endpoint_accessible_without_api_key(self):
        response = self._client().get("/internal/health")
        assert response.status_code == 200
        body = response.json()
        assert body["status"] == "UP"
        assert "ocr" in body["modules"]
