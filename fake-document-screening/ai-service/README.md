# VeriLens — AI Service

Python 3.11+ / FastAPI service providing AI/CV analysis capabilities.

## Status

**Not yet initialized.** The FastAPI project will be created in Phase 1.

## Responsibilities

- Image preprocessing (resize, normalize, denoise)
- OCR (PaddleOCR or Tesseract)
- Image forensics analysis
- Metadata extraction where appropriate
- Face detection and embedding comparison
- AI manipulation model inference
- Structured JSON responses

## Technology

| Tool             | Purpose                          |
| ---------------- | -------------------------------- |
| Python 3.11+     | Language                         |
| FastAPI          | HTTP framework                   |
| Uvicorn          | ASGI server                      |
| PaddleOCR / Tesseract | OCR engine                  |
| OpenCV           | Image processing                 |
| Pillow           | Image I/O                        |
| PyMuPDF          | PDF processing                   |
| scikit-learn     | Classical ML where needed        |
| PyTorch          | Deep learning where needed       |

## Communication

This service is called **only** by the Spring Boot backend via internal REST.  
It must **never** be exposed directly to the frontend.  
It must **never** write directly to PostgreSQL.
