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
from app.modules import metadata as metadata_module
from app.modules import identity as identity_module
from app.modules import forensics as forensics_module

API_KEY = os.getenv("AI_SERVICE_API_KEY", "dev-api-key")

app = FastAPI(
    title="VeriLens AI Service",
    description="Internal AI/CV analysis service for VeriLens document screening.",
    version="0.5.0",
    docs_url="/internal/docs",
    redoc_url=None,
)


# ---------------------------------------------------------------------------
# Middleware — API key verification
# ---------------------------------------------------------------------------
@app.middleware("http")
async def verify_api_key(request: Request, call_next):
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
    return {
        "status": "UP",
        "service": "verilens-ai-service",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "modules": {
            "ocr":              "AVAILABLE" if ocr_module.is_available() else "UNAVAILABLE",
            "metadata":         "AVAILABLE",
            "identity":         "AVAILABLE",
            "forensics":        "AVAILABLE" if forensics_module.is_available() else "UNAVAILABLE",
            "face":             "UNAVAILABLE",
            "aiManipulation":   "UNAVAILABLE",
        },
    }


# ---------------------------------------------------------------------------
# Analyze endpoint — Phases 4–7
# ---------------------------------------------------------------------------
@app.post("/internal/analyze")
async def analyze(
    file: UploadFile = File(...),
    documentId: str = Form(...),
    modules: str = Form(default="OCR,METADATA,IDENTITY,FORENSICS"),
):
    """
    Run analysis modules on the uploaded document.

    Phase 4: OCR (Tesseract)
    Phase 5: Metadata analysis (Pillow + PyMuPDF)
    Phase 6: Identity consistency (rule-based, depends on OCR output)
    Phase 7: Image forensics (OpenCV — ELA, noise, edge analysis)
    Other modules return NOT_APPLICABLE until their phases are implemented.
    """
    start = time.time()

    file_bytes = await file.read()
    mime_type = file.content_type or "application/octet-stream"
    requested = {m.strip().upper() for m in modules.split(",")} if modules else {"OCR", "METADATA", "IDENTITY", "FORENSICS"}

    module_results: dict = {}

    # --- OCR (Phase 4) ---
    if "OCR" in requested:
        module_results["ocr"] = ocr_module.run(file_bytes, mime_type)
    else:
        module_results["ocr"] = {"status": "NOT_APPLICABLE"}

    # --- Metadata (Phase 5) ---
    if "METADATA" in requested:
        module_results["metadata"] = metadata_module.run(file_bytes, mime_type)
    else:
        module_results["metadata"] = {"status": "NOT_APPLICABLE"}

    # --- Identity Consistency (Phase 6) — depends on OCR output ---
    if "IDENTITY" in requested:
        ocr_output = module_results.get("ocr", {})
        module_results["identity"] = identity_module.run(ocr_output)
    else:
        module_results["identity"] = {"status": "NOT_APPLICABLE"}

    # --- Image Forensics (Phase 7) ---
    if "FORENSICS" in requested:
        module_results["forensics"] = forensics_module.run(file_bytes, mime_type)
    else:
        module_results["forensics"] = {"status": "NOT_APPLICABLE"}

    # --- Future modules — NOT_APPLICABLE until implemented ---
    for key in ("faceVerification", "aiManipulation"):
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
