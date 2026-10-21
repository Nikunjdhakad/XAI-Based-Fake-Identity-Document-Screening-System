"""
Pydantic schemas for the /internal/analyze request and response.
"""

from typing import Optional
from pydantic import BaseModel


class OcrResult(BaseModel):
    status: str  # SUCCESS | FAILED | NOT_APPLICABLE
    extractedText: Optional[str] = None
    words: list = []
    overallConfidence: float = 0.0
    pageCount: int = 0
    processingTimeMs: int = 0
    error: Optional[str] = None


class AnalysisResponse(BaseModel):
    documentId: str
    status: str  # COMPLETED | PARTIALLY_COMPLETED | FAILED
    modules: dict
    processingTimeMs: int
