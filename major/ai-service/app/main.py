"""
VeriLens AI Service — FastAPI Application Entry Point.

Internal service called ONLY by Spring Boot via /internal/* endpoints.
Never exposed directly to the frontend.
"""

import os
import time
from datetime import datetime, timezone

from fastapi import FastAPI, File, Form, Request, UploadFile
from fastapi.responses import JSONResponse

from app.modules import ocr as ocr_module

API_KEY = os.getenv("AI_SERVICE_API_KEY", "dev-api-key")

app = FastAPI(
    title="VeriLens AI Service",
    description="Internal AI/CV analysis service for VeriLens document screening.",
    version="0.2.0",
    docs_url="/internal/docs",
    redoc_url=None,
)


# ---------------------------------------------------------------------------
# Middleware — API key verification for all internal endpoints except health
# ---------------------------------------------------------------------------
@app.middleware("http")
async def verify_api_key(request: Request, call_next):
    """
    Verify the X-API-Key header for all /internal/* paths except /internal/health.
    The health endpoint is exempt to allow Docker and Spring Boot health checks.
    """
    if (
        request.url.path.startswith("/internal/")
        and request.url.path != "/internal/health"
    ):
        provided_key = request.headers.get("X-API-Key", "")
        if provided_key != API_KEY:
            return JSONResponse(
                status_code=401,
                content={"detail": "Invalid or missing API key"},
            )
    response = await call_next(request)
    return response


# ---------------------------------------------------------------------------
# Health endpoint
# ---------------------------------------------------------------------------
@app.get("/internal/health")
async def health():
    """
    Reports service status and per-module availability.
    OCR is AVAILABLE when Tesseract is installed and reachable.
    """
    return {
        "status": "UP",
        "service": "verilens-ai-service",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "modules": {
            "ocr": "AVAILABLE" if ocr_module.is_available() else "UNAVAILABLE",
            "forensics": "UNAVAILABLE",
            "metadata": "UNAVAILABLE",
            "face": "UNAVAILABLE",
            "aiManipulation": "UNAVAILABLE",
        },
    }


# ---------------------------------------------------------------------------
# Analyze endpoint — Phase 4: OCR connected
# ---------------------------------------------------------------------------
@app.post("/internal/analyze")
async def analyze(
    file: UploadFile = File(...),
    documentId: str = Form(...),
    modules: str = Form(default="OCR"),
):
    """
    Run analysis modules on the uploaded document.

    Phase 4: OCR is fully implemented.
    Other modules return NOT_APPLICABLE until their phases are implemented.
    The module list is comma-separated (e.g., "OCR,METADATA").
    """
    start = time.time()

    file_bytes = await file.read()
    mime_type = file.content_type or "application/octet-stream"
    requested = {m.strip().upper() for m in modules.split(",")} if modules else {"OCR"}

    module_results: dict = {}

    # --- OCR (Phase 4) ---
    if "OCR" in requested:
        module_results["ocr"] = ocr_module.run(file_bytes, mime_type)
    else:
        module_results["ocr"] = {"status": "NOT_APPLICABLE"}

    # --- Future modules — NOT_APPLICABLE until implemented ---
    for key in ("forensics", "metadata", "identity", "faceVerification", "aiManipulation"):
        module_results[key] = {"status": "NOT_APPLICABLE"}

    # Determine overall status
    statuses = [v.get("status", "FAILED") for v in module_results.values()]
    if all(s == "NOT_APPLICABLE" for s in statuses):
        overall = "FAILED"
    elif all(s in ("SUCCESS", "NOT_APPLICABLE") for s in statuses):
        overall = "COMPLETED"
    elif any(s == "SUCCESS" for s in statuses):
        overall = "PARTIALLY_COMPLETED"
    else:
        overall = "FAILED"

    return {
        "documentId": documentId,
        "status": overall,
        "modules": module_results,
        "processingTimeMs": int((time.time() - start) * 1000),
    }
