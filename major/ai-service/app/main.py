"""
VeriLens AI Service — FastAPI Application Entry Point.

This is the Python AI/CV inference service. It is called ONLY
by the Spring Boot backend via internal REST endpoints.
It must never be exposed directly to the frontend.
"""

import os
from datetime import datetime, timezone

from fastapi import FastAPI, Request, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

API_KEY = os.getenv("AI_SERVICE_API_KEY", "dev-api-key")

app = FastAPI(
    title="VeriLens AI Service",
    description="Internal AI/CV analysis service for VeriLens document screening.",
    version="0.1.0",
    docs_url="/internal/docs",
    redoc_url=None,
)


# ---------------------------------------------------------------------------
# Middleware — API key verification for internal endpoints
# ---------------------------------------------------------------------------
@app.middleware("http")
async def verify_api_key(request: Request, call_next):
    """
    Simple API key check for internal endpoints.
    The /internal/health endpoint is exempt to allow Docker health checks.
    """
    if request.url.path.startswith("/internal/") and request.url.path != "/internal/health":
        provided_key = request.headers.get("X-API-Key", "")
        if provided_key != API_KEY:
            return JSONResponse(
                status_code=401,
                content={"detail": "Invalid or missing API key"},
            )
    response = await call_next(request)
    return response


# ---------------------------------------------------------------------------
# Health Endpoint
# ---------------------------------------------------------------------------
@app.get("/internal/health")
async def health():
    """
    Health check endpoint.
    Reports service status and module availability.
    Modules will be marked AVAILABLE as they are implemented in later phases.
    """
    return {
        "status": "UP",
        "service": "verilens-ai-service",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "modules": {
            "ocr": "UNAVAILABLE",
            "forensics": "UNAVAILABLE",
            "metadata": "UNAVAILABLE",
            "face": "UNAVAILABLE",
            "aiManipulation": "UNAVAILABLE",
        },
    }


# ---------------------------------------------------------------------------
# Placeholder for future /internal/analyze endpoint (Phase 4+)
# ---------------------------------------------------------------------------
# POST /internal/analyze will be implemented when the first analysis
# module (OCR) is built in Phase 4. It is documented in api-contract.md
# but not stubbed here to avoid fake/mock responses.
