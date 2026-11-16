FROM python:3.11-alpine

# Install Tesseract OCR engine + poppler (PDF page rendering) via apk
# Alpine repos are not blocked by common corporate proxies
RUN apk add --no-cache \
    tesseract-ocr \
    tesseract-ocr-data-eng \
    poppler-utils \
    # Build deps for numpy / opencv
    gcc \
    g++ \
    musl-dev \
    linux-headers \
    cmake \
    make \
    && pip install --no-cache-dir --upgrade pip

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt \
    && apk del gcc g++ musl-dev linux-headers cmake make

COPY app ./app

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
