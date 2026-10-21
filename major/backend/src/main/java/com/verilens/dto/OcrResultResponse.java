package com.verilens.dto;

import com.verilens.entity.OcrResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * OCR result DTO — returned by GET /api/documents/{id}/analysis/ocr.
 * Does not expose the raw words_json blob; that is internal storage.
 */
public class OcrResultResponse {

    private UUID id;
    private String status;
    private String extractedText;
    private BigDecimal overallConfidence;
    private Integer pageCount;
    private String errorMessage;
    private Long processingTimeMs;
    private Instant createdAt;

    public OcrResultResponse() {}

    public static OcrResultResponse fromEntity(OcrResult ocr) {
        OcrResultResponse dto = new OcrResultResponse();
        dto.setId(ocr.getId());
        dto.setStatus(ocr.getStatus());
        dto.setExtractedText(ocr.getExtractedText());
        dto.setOverallConfidence(ocr.getOverallConfidence());
        dto.setPageCount(ocr.getPageCount());
        dto.setErrorMessage(ocr.getErrorMessage());
        dto.setProcessingTimeMs(ocr.getProcessingTimeMs());
        dto.setCreatedAt(ocr.getCreatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public BigDecimal getOverallConfidence() { return overallConfidence; }
    public void setOverallConfidence(BigDecimal overallConfidence) { this.overallConfidence = overallConfidence; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
