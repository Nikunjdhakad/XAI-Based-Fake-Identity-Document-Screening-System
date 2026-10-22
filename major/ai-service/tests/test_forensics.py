"""
Tests for the Image Forensics module — Phase 7.
Uses synthetic PIL images.
"""

import io
import numpy as np
import pytest
from PIL import Image, ImageDraw


def _make_test_image(width=400, height=300) -> bytes:
    """Create a simple test image."""
    img = Image.new("RGB", (width, height), color="white")
    draw = ImageDraw.Draw(img)
    draw.text((20, 20), "Test Document Name: John", fill="black")
    draw.rectangle([100, 100, 200, 200], outline="black", width=2)
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=90)
    return buf.getvalue()


def _make_png_bytes() -> bytes:
    img = Image.new("RGB", (400, 300), color="white")
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


# ---------------------------------------------------------------------------
# Availability
# ---------------------------------------------------------------------------

class TestForensicsAvailability:
    def test_is_available_returns_bool(self):
        from app.modules.forensics import is_available
        assert isinstance(is_available(), bool)


# ---------------------------------------------------------------------------
# run() output schema
# ---------------------------------------------------------------------------

class TestForensicsRunSchema:
    def test_returns_dict(self):
        from app.modules.forensics import run
        result = run(_make_test_image(), "image/jpeg")
        assert isinstance(result, dict)

    def test_has_required_keys(self):
        from app.modules.forensics import run
        result = run(_make_test_image(), "image/jpeg")
        for key in ("status", "processingTimeMs", "riskScore", "findings", "suspiciousRegions"):
            assert key in result, f"Missing key: {key}"

    def test_status_is_valid(self):
        from app.modules.forensics import run
        result = run(_make_test_image(), "image/jpeg")
        assert result["status"] in ("SUCCESS", "FAILED", "NOT_APPLICABLE")

    def test_processing_time_non_negative(self):
        from app.modules.forensics import run
        result = run(_make_test_image(), "image/jpeg")
        assert result["processingTimeMs"] >= 0

    def test_risk_score_between_0_and_1(self):
        from app.modules.forensics import run
        result = run(_make_test_image(), "image/jpeg")
        if result["status"] == "SUCCESS":
            assert 0.0 <= result["riskScore"] <= 1.0

    def test_findings_is_list(self):
        from app.modules.forensics import run
        result = run(_make_test_image(), "image/jpeg")
        assert isinstance(result["findings"], list)

    def test_suspicious_regions_is_list(self):
        from app.modules.forensics import run
        result = run(_make_test_image(), "image/jpeg")
        assert isinstance(result["suspiciousRegions"], list)

    def test_png_image_works(self):
        from app.modules.forensics import run
        result = run(_make_png_bytes(), "image/png")
        assert result["status"] in ("SUCCESS", "FAILED")

    def test_invalid_bytes_returns_failed(self):
        from app.modules.forensics import run
        result = run(b"not an image", "image/jpeg")
        assert result["status"] == "FAILED"


# ---------------------------------------------------------------------------
# Findings structure
# ---------------------------------------------------------------------------

class TestForensicsFindings:
    def test_each_finding_has_required_fields(self):
        from app.modules.forensics import run
        result = run(_make_test_image(), "image/jpeg")
        if result["status"] == "SUCCESS":
            for f in result["findings"]:
                assert "type" in f
                assert "severity" in f
                assert "description" in f
                assert "confidence" in f
                assert f["severity"] in ("LOW", "MEDIUM", "HIGH")

    def test_finding_types_are_known(self):
        from app.modules.forensics import run
        result = run(_make_test_image(), "image/jpeg")
        if result["status"] == "SUCCESS":
            known = {"ERROR_LEVEL_ANALYSIS", "NOISE_INCONSISTENCY",
                     "EDGE_DENSITY_ANOMALY", "NO_ANOMALIES_DETECTED"}
            for f in result["findings"]:
                assert f["type"] in known, f"Unknown finding type: {f['type']}"


# ---------------------------------------------------------------------------
# Suspicious regions structure
# ---------------------------------------------------------------------------

class TestForensicsRegions:
    def test_regions_have_coordinates(self):
        from app.modules.forensics import run
        result = run(_make_test_image(), "image/jpeg")
        for r in result["suspiciousRegions"]:
            assert "x" in r and "y" in r
            assert "width" in r and "height" in r
            assert isinstance(r["x"], int)
            assert isinstance(r["y"], int)


# ---------------------------------------------------------------------------
# /internal/analyze endpoint integration
# ---------------------------------------------------------------------------

class TestAnalyzeEndpointForensics:
    def _client(self):
        from fastapi.testclient import TestClient
        from app.main import app
        return TestClient(app)

    def _api_key(self):
        from app.main import API_KEY
        return API_KEY

    def test_forensics_key_present_when_requested(self):
        body = self._client().post(
            "/internal/analyze",
            headers={"X-API-Key": self._api_key()},
            data={"documentId": "forensics-001", "modules": "OCR,FORENSICS"},
            files={"file": ("doc.jpg", _make_test_image(), "image/jpeg")},
        ).json()
        assert "forensics" in body["modules"]

    def test_forensics_status_is_valid(self):
        body = self._client().post(
            "/internal/analyze",
            headers={"X-API-Key": self._api_key()},
            data={"documentId": "forensics-002", "modules": "FORENSICS"},
            files={"file": ("doc.jpg", _make_test_image(), "image/jpeg")},
        ).json()
        assert body["modules"]["forensics"]["status"] in ("SUCCESS", "FAILED")

    def test_forensics_not_run_when_not_in_modules(self):
        body = self._client().post(
            "/internal/analyze",
            headers={"X-API-Key": self._api_key()},
            data={"documentId": "forensics-003", "modules": "OCR"},
            files={"file": ("doc.jpg", _make_test_image(), "image/jpeg")},
        ).json()
        assert body["modules"]["forensics"]["status"] == "NOT_APPLICABLE"

    def test_health_reports_forensics_status(self):
        body = self._client().get("/internal/health").json()
        assert body["modules"]["forensics"] in ("AVAILABLE", "UNAVAILABLE")
