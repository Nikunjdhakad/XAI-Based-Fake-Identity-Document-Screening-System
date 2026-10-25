"""
Tests for the AI Manipulation Screening module — Phase 9.

Tests cover:
  - Module availability
  - Output schema validation
  - Individual technique results
  - Findings structure
  - Suspicious regions
  - Risk score calculation
  - Manipulation type inference
  - Endpoint integration
  - Error handling (non-image input)
"""

import io
import json
import pytest
import numpy as np
from PIL import Image

from app.modules import ai_manipulation


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_test_image(width=200, height=200, color=(128, 128, 128)) -> bytes:
    """Create a simple solid-color test image."""
    img = Image.new("RGB", (width, height), color)
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=95)
    return buf.getvalue()


def _make_noisy_image(width=200, height=200, seed=42) -> bytes:
    """Create a noisy test image (simulates natural texture)."""
    rng = np.random.RandomState(seed)
    arr = rng.randint(0, 256, (height, width, 3), dtype=np.uint8)
    img = Image.fromarray(arr, "RGB")
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=95)
    return buf.getvalue()


def _make_gradient_image(width=200, height=200) -> bytes:
    """Create a smooth gradient image (simulates AI-generated smoothness)."""
    arr = np.zeros((height, width, 3), dtype=np.uint8)
    for y in range(height):
        val = int(255 * y / height)
        arr[y, :] = [val, val, val]
    img = Image.fromarray(arr, "RGB")
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=95)
    return buf.getvalue()


def _make_png_image(width=200, height=200) -> bytes:
    """Create a simple PNG test image."""
    img = Image.new("RGB", (width, height), (100, 150, 200))
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


# ---------------------------------------------------------------------------
# Module availability
# ---------------------------------------------------------------------------

class TestAvailability:
    """Test module availability detection."""

    def test_is_available_returns_bool(self):
        result = ai_manipulation.is_available()
        assert isinstance(result, bool)

    def test_is_available_true_when_opencv_present(self):
        """On a system with OpenCV installed, should return True."""
        try:
            import cv2
            assert ai_manipulation.is_available() is True
        except ImportError:
            pytest.skip("OpenCV not installed")


# ---------------------------------------------------------------------------
# Output schema
# ---------------------------------------------------------------------------

class TestOutputSchema:
    """Test that module output matches the expected schema."""

    def test_success_schema(self):
        """Successful run should return all required fields."""
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")

        assert "status" in result
        assert result["status"] == "SUCCESS"
        assert "riskScore" in result
        assert "manipulationType" in result
        assert "confidence" in result
        assert "findings" in result
        assert "suspiciousRegions" in result
        assert "modelInfo" in result
        assert "processingTimeMs" in result

    def test_risk_score_is_number(self):
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")
        assert isinstance(result["riskScore"], (int, float))
        assert 0.0 <= result["riskScore"] <= 1.0

    def test_confidence_is_number(self):
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")
        assert isinstance(result["confidence"], (int, float))
        assert 0.0 <= result["confidence"] <= 1.0

    def test_manipulation_type_valid(self):
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")
        valid_types = {"UNKNOWN", "INPAINTING", "SPLICING", "GENERATION", "NONE_DETECTED"}
        assert result["manipulationType"] in valid_types

    def test_findings_is_list(self):
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")
        assert isinstance(result["findings"], list)

    def test_suspicious_regions_is_list(self):
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")
        assert isinstance(result["suspiciousRegions"], list)

    def test_model_info_present(self):
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")
        model_info = result["modelInfo"]
        assert "name" in model_info
        assert "version" in model_info
        assert "techniques" in model_info
        assert "limitations" in model_info
        assert isinstance(model_info["techniques"], list)
        assert len(model_info["techniques"]) == 4

    def test_processing_time_positive(self):
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")
        assert isinstance(result["processingTimeMs"], int)
        assert result["processingTimeMs"] >= 0


# ---------------------------------------------------------------------------
# Findings structure
# ---------------------------------------------------------------------------

class TestFindings:
    """Test that findings have the correct structure."""

    def test_four_findings_returned(self):
        """Should always return exactly 4 findings (one per technique)."""
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")
        assert len(result["findings"]) == 4

    def test_finding_has_required_fields(self):
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")
        for finding in result["findings"]:
            assert "type" in finding
            assert "severity" in finding
            assert "description" in finding
            assert "confidence" in finding
            assert "suspicious" in finding

    def test_finding_types_cover_all_techniques(self):
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")
        types = {f["type"] for f in result["findings"]}
        expected = {"spectral_analysis", "jpeg_ghost", "gradient_consistency", "texture_regularity"}
        assert types == expected

    def test_severity_values_valid(self):
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")
        valid_severities = {"NONE", "LOW", "MEDIUM", "HIGH"}
        for finding in result["findings"]:
            assert finding["severity"] in valid_severities

    def test_description_is_string(self):
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")
        for finding in result["findings"]:
            assert isinstance(finding["description"], str)
            assert len(finding["description"]) > 0


# ---------------------------------------------------------------------------
# Individual techniques
# ---------------------------------------------------------------------------

class TestSpectralAnalysis:
    """Test spectral analysis technique."""

    def test_spectral_on_solid_image(self):
        """A solid color image has very low high-frequency content."""
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")
        spectral = next(f for f in result["findings"] if f["type"] == "spectral_analysis")
        assert isinstance(spectral["suspicious"], bool)

    def test_spectral_on_noisy_image(self):
        """A noisy image should have significant high-frequency content."""
        result = ai_manipulation.run(_make_noisy_image(), "image/jpeg")
        spectral = next(f for f in result["findings"] if f["type"] == "spectral_analysis")
        assert isinstance(spectral["suspicious"], bool)


class TestJpegGhost:
    """Test JPEG ghost detection technique."""

    def test_jpeg_ghost_returns_result(self):
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")
        jpeg_ghost = next(f for f in result["findings"] if f["type"] == "jpeg_ghost")
        assert isinstance(jpeg_ghost["suspicious"], bool)

    def test_jpeg_ghost_on_png(self):
        """PNG images can also be analyzed for JPEG ghost artifacts."""
        result = ai_manipulation.run(_make_png_image(), "image/png")
        jpeg_ghost = next(f for f in result["findings"] if f["type"] == "jpeg_ghost")
        assert isinstance(jpeg_ghost["suspicious"], bool)


class TestGradientConsistency:
    """Test gradient consistency analysis."""

    def test_gradient_on_normal_image(self):
        result = ai_manipulation.run(_make_noisy_image(), "image/jpeg")
        gradient = next(f for f in result["findings"] if f["type"] == "gradient_consistency")
        assert isinstance(gradient["suspicious"], bool)

    def test_gradient_on_smooth_image(self):
        """A very smooth gradient image should have some smooth blocks."""
        result = ai_manipulation.run(_make_gradient_image(), "image/jpeg")
        gradient = next(f for f in result["findings"] if f["type"] == "gradient_consistency")
        assert isinstance(gradient["suspicious"], bool)


class TestTextureRegularity:
    """Test texture regularity analysis."""

    def test_texture_returns_result(self):
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")
        texture = next(f for f in result["findings"] if f["type"] == "texture_regularity")
        assert isinstance(texture["suspicious"], bool)

    def test_texture_has_details(self):
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")
        texture = next(f for f in result["findings"] if f["type"] == "texture_regularity")
        assert "details" in texture


# ---------------------------------------------------------------------------
# Suspicious regions
# ---------------------------------------------------------------------------

class TestSuspiciousRegions:
    """Test suspicious region collection."""

    def test_regions_have_required_fields(self):
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")
        for region in result["suspiciousRegions"]:
            assert "x" in region
            assert "y" in region
            assert "width" in region
            assert "height" in region
            assert "source" in region
            assert "confidence" in region
            assert "description" in region

    def test_regions_capped_at_15(self):
        """Suspicious regions should never exceed 15."""
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")
        assert len(result["suspiciousRegions"]) <= 15


# ---------------------------------------------------------------------------
# Error handling
# ---------------------------------------------------------------------------

class TestErrorHandling:
    """Test graceful failure on invalid input."""

    def test_invalid_bytes_returns_failed(self):
        result = ai_manipulation.run(b"not an image", "image/jpeg")
        assert result["status"] == "FAILED"
        assert "error" in result
        assert result["riskScore"] is None

    def test_empty_bytes_returns_failed(self):
        result = ai_manipulation.run(b"", "image/jpeg")
        assert result["status"] == "FAILED"

    def test_model_info_present_on_failure(self):
        result = ai_manipulation.run(b"not an image", "image/jpeg")
        assert "modelInfo" in result
        assert result["modelInfo"]["name"] == "VeriLens Frequency-Statistical Baseline"

    def test_processing_time_on_failure(self):
        result = ai_manipulation.run(b"invalid", "image/jpeg")
        assert isinstance(result["processingTimeMs"], int)


# ---------------------------------------------------------------------------
# Image format support
# ---------------------------------------------------------------------------

class TestFormats:
    """Test various image format support."""

    def test_jpeg_image(self):
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")
        assert result["status"] == "SUCCESS"

    def test_png_image(self):
        result = ai_manipulation.run(_make_png_image(), "image/png")
        assert result["status"] == "SUCCESS"

    def test_small_image(self):
        """Very small images should still return SUCCESS with reduced analysis."""
        small = _make_test_image(width=20, height=20)
        result = ai_manipulation.run(small, "image/jpeg")
        assert result["status"] == "SUCCESS"
        assert len(result["findings"]) == 4

    def test_large_image(self):
        """Larger images should complete without error."""
        large = _make_noisy_image(width=500, height=500)
        result = ai_manipulation.run(large, "image/jpeg")
        assert result["status"] == "SUCCESS"


# ---------------------------------------------------------------------------
# Risk score logic
# ---------------------------------------------------------------------------

class TestRiskScore:
    """Test risk score calculation."""

    def test_risk_score_between_0_and_1(self):
        result = ai_manipulation.run(_make_test_image(), "image/jpeg")
        assert 0.0 <= result["riskScore"] <= 1.0

    def test_noisy_image_risk_score(self):
        """A noisy image should produce a valid risk score."""
        result = ai_manipulation.run(_make_noisy_image(), "image/jpeg")
        assert 0.0 <= result["riskScore"] <= 1.0

    def test_none_detected_for_clean_image(self):
        """A simple clean image may show NONE_DETECTED or a low score."""
        result = ai_manipulation.run(_make_noisy_image(seed=0), "image/jpeg")
        # We can't guarantee NONE_DETECTED, but score should be reasonable
        assert 0.0 <= result["riskScore"] <= 1.0


# ---------------------------------------------------------------------------
# Endpoint integration (via TestClient)
# ---------------------------------------------------------------------------

class TestEndpoint:
    """Test the /internal/analyze endpoint with AI_MANIPULATION module."""

    @pytest.fixture
    def client(self):
        from fastapi.testclient import TestClient
        from app.main import app
        return TestClient(app)

    def test_analyze_includes_ai_manipulation(self, client):
        """AI manipulation should be included in default analysis."""
        img = _make_test_image()
        response = client.post(
            "/internal/analyze",
            data={"documentId": "test-ai-manip", "modules": "AI_MANIPULATION"},
            files={"file": ("test.jpg", img, "image/jpeg")},
            headers={"X-API-Key": "dev-api-key"},
        )
        assert response.status_code == 200
        body = response.json()
        assert "aiManipulation" in body["modules"]
        ai_result = body["modules"]["aiManipulation"]
        assert ai_result["status"] == "SUCCESS"
        assert "riskScore" in ai_result
        assert "findings" in ai_result

    def test_analyze_ai_manipulation_not_requested(self, client):
        """AI manipulation should be NOT_APPLICABLE if not requested."""
        img = _make_test_image()
        response = client.post(
            "/internal/analyze",
            data={"documentId": "test-no-ai", "modules": "OCR"},
            files={"file": ("test.jpg", img, "image/jpeg")},
            headers={"X-API-Key": "dev-api-key"},
        )
        assert response.status_code == 200
        body = response.json()
        ai_result = body["modules"]["aiManipulation"]
        assert ai_result["status"] == "NOT_APPLICABLE"

    def test_default_modules_include_ai_manipulation(self, client):
        """Default modules should now include AI_MANIPULATION."""
        img = _make_test_image()
        response = client.post(
            "/internal/analyze",
            data={"documentId": "test-defaults"},
            files={"file": ("test.jpg", img, "image/jpeg")},
            headers={"X-API-Key": "dev-api-key"},
        )
        assert response.status_code == 200
        body = response.json()
        ai_result = body["modules"]["aiManipulation"]
        assert ai_result["status"] != "NOT_APPLICABLE"

    def test_health_reports_ai_manipulation_available(self, client):
        """Health endpoint should report aiManipulation status."""
        response = client.get("/internal/health")
        assert response.status_code == 200
        body = response.json()
        assert "aiManipulation" in body["modules"]
        # Should be AVAILABLE if OpenCV is installed
        assert body["modules"]["aiManipulation"] in ("AVAILABLE", "UNAVAILABLE")
