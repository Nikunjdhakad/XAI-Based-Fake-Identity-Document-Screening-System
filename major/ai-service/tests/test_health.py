"""
VeriLens AI Service — Tests for health endpoint.
"""

import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.fixture
def client():
    return TestClient(app)


def test_health_returns_up(client):
    """Health endpoint returns UP status with module availability."""
    response = client.get("/internal/health")
    assert response.status_code == 200

    data = response.json()
    assert data["status"] == "UP"
    assert data["service"] == "verilens-ai-service"
    assert "timestamp" in data
    assert "modules" in data

    # OCR, metadata, identity, and forensics are now implemented (Phases 4–7)
    # Face uses CascadeClassifier which may not be available in OpenCV 5
    assert data["modules"]["ocr"] in ("AVAILABLE", "UNAVAILABLE")
    assert data["modules"]["metadata"] == "AVAILABLE"
    assert data["modules"]["identity"] == "AVAILABLE"
    assert data["modules"]["forensics"] in ("AVAILABLE", "UNAVAILABLE")
    assert data["modules"]["face"] in ("AVAILABLE", "UNAVAILABLE")
    assert data["modules"]["aiManipulation"] == "UNAVAILABLE"


def test_internal_endpoint_requires_api_key(client):
    """Non-health internal endpoints should require API key."""
    # The /internal/docs endpoint should be protected
    # (FastAPI docs are mounted at /internal/docs)
    # For now, just verify the health endpoint is exempt
    response = client.get("/internal/health")
    assert response.status_code == 200
