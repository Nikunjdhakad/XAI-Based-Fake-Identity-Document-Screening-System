"""
Tests for the Metadata Analysis module — Phase 5.
Uses synthetic PIL images and minimal in-memory PDFs.
"""

import io
import pytest
from PIL import Image, ImageDraw


def _make_png_bytes() -> bytes:
    img = Image.new("RGB", (300, 200), color="white")
    draw = ImageDraw.Draw(img)
    draw.text((10, 10), "Test Document", fill="black")
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


def _make_jpeg_bytes() -> bytes:
    img = Image.new("RGB", (300, 200), color="white")
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=85)
    return buf.getvalue()


def _make_minimal_pdf_bytes() -> bytes:
    """Create a minimal valid PDF in memory using PyMuPDF if available."""
    try:
        import fitz
        doc = fitz.open()
        page = doc.new_page(width=595, height=842)
        page.insert_text((50, 100), "Test document content", fontsize=12)
        buf = io.BytesIO()
        doc.save(buf)
        doc.close()
        return buf.getvalue()
    except Exception:
        # Return a minimal static PDF if PyMuPDF not available
        return b"%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R>>endobj\nxref\n0 4\n0000000000 65535 f\n0000000009 00000 n\n0000000058 00000 n\n0000000115 00000 n\ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n190\n%%EOF"


# ---------------------------------------------------------------------------
# Module-level tests
# ---------------------------------------------------------------------------

class TestMetadataRunSchema:
    def test_returns_dict(self):
        from app.modules.metadata import run
        result = run(_make_png_bytes(), "image/png")
        assert isinstance(result, dict)

    def test_has_required_keys(self):
        from app.modules.metadata import run
        result = run(_make_png_bytes(), "image/png")
        for key in ("status", "processingTimeMs"):
            assert key in result, f"Missing key: {key}"

    def test_status_is_valid(self):
        from app.modules.metadata import run
        result = run(_make_png_bytes(), "image/png")
        assert result["status"] in ("SUCCESS", "FAILED", "NOT_APPLICABLE")

    def test_processing_time_non_negative(self):
        from app.modules.metadata import run
        result = run(_make_png_bytes(), "image/png")
        assert result["processingTimeMs"] >= 0

    def test_png_returns_success(self):
        from app.modules.metadata import run
        result = run(_make_png_bytes(), "image/png")
        assert result["status"] == "SUCCESS"

    def test_jpeg_returns_success(self):
        from app.modules.metadata import run
        result = run(_make_jpeg_bytes(), "image/jpeg")
        assert result["status"] == "SUCCESS"

    def test_properties_is_dict(self):
        from app.modules.metadata import run
        result = run(_make_png_bytes(), "image/png")
        assert isinstance(result.get("properties", {}), dict)

    def test_anomalies_is_list(self):
        from app.modules.metadata import run
        result = run(_make_png_bytes(), "image/png")
        assert isinstance(result.get("anomalies", []), list)

    def test_evidence_score_between_0_and_1(self):
        from app.modules.metadata import run
        result = run(_make_png_bytes(), "image/png")
        if result["status"] == "SUCCESS":
            score = result.get("evidenceScore", 0.0)
            assert 0.0 <= score <= 1.0

    def test_unsupported_mime_returns_not_applicable(self):
        from app.modules.metadata import run
        result = run(b"some binary data", "application/octet-stream")
        assert result["status"] == "NOT_APPLICABLE"

    def test_invalid_bytes_returns_failed(self):
        from app.modules.metadata import run
        result = run(b"not an image", "image/jpeg")
        assert result["status"] == "FAILED"

    def test_pdf_returns_success_or_failed(self):
        from app.modules.metadata import run
        pdf_bytes = _make_minimal_pdf_bytes()
        result = run(pdf_bytes, "application/pdf")
        assert result["status"] in ("SUCCESS", "FAILED", "NOT_APPLICABLE")

    def test_dimensions_present_for_image(self):
        from app.modules.metadata import run
        result = run(_make_png_bytes(), "image/png")
        if result["status"] == "SUCCESS":
            dims = result.get("properties", {}).get("dimensions")
            if dims:
                assert "width" in dims and "height" in dims


class TestAnomalyStructure:
    def test_each_anomaly_has_required_fields(self):
        from app.modules.metadata import run
        # Use a JPEG (stripped EXIF) to trigger LOW anomaly
        result = run(_make_jpeg_bytes(), "image/jpeg")
        for anomaly in result.get("anomalies", []):
            assert "type" in anomaly
            assert "severity" in anomaly
            assert "description" in anomaly
            assert anomaly["severity"] in ("LOW", "MEDIUM", "HIGH")


class TestEvidenceScore:
    def test_no_anomalies_gives_zero_score(self):
        from app.modules.metadata import _compute_evidence_score
        assert _compute_evidence_score([]) == 0.0

    def test_high_anomaly_gives_high_score(self):
        from app.modules.metadata import _compute_evidence_score
        anomalies = [{"severity": "HIGH"}, {"severity": "HIGH"}, {"severity": "HIGH"}]
        score = _compute_evidence_score(anomalies)
        assert score > 0.5

    def test_score_capped_at_1(self):
        from app.modules.metadata import _compute_evidence_score
        anomalies = [{"severity": "HIGH"}] * 10
        assert _compute_evidence_score(anomalies) == 1.0


# ---------------------------------------------------------------------------
# /internal/analyze endpoint integration
# ---------------------------------------------------------------------------

class TestAnalyzeEndpointMetadata:
    def _client(self):
        from fastapi.testclient import TestClient
        from app.main import app
        return TestClient(app)

    def _api_key(self):
        from app.main import API_KEY
        return API_KEY

    def test_metadata_key_present_when_requested(self):
        body = self._client().post(
            "/internal/analyze",
            headers={"X-API-Key": self._api_key()},
            data={"documentId": "meta-001", "modules": "OCR,METADATA"},
            files={"file": ("doc.png", _make_png_bytes(), "image/png")},
        ).json()
        assert "metadata" in body["modules"]

    def test_metadata_status_is_valid(self):
        body = self._client().post(
            "/internal/analyze",
            headers={"X-API-Key": self._api_key()},
            data={"documentId": "meta-002", "modules": "OCR,METADATA"},
            files={"file": ("doc.png", _make_png_bytes(), "image/png")},
        ).json()
        assert body["modules"]["metadata"]["status"] in ("SUCCESS", "FAILED", "NOT_APPLICABLE")

    def test_metadata_not_run_when_not_in_modules_list(self):
        body = self._client().post(
            "/internal/analyze",
            headers={"X-API-Key": self._api_key()},
            data={"documentId": "meta-003", "modules": "OCR"},
            files={"file": ("doc.png", _make_png_bytes(), "image/png")},
        ).json()
        assert body["modules"]["metadata"]["status"] == "NOT_APPLICABLE"

    def test_health_reports_metadata_available(self):
        body = self._client().get("/internal/health").json()
        assert body["modules"]["metadata"] == "AVAILABLE"
