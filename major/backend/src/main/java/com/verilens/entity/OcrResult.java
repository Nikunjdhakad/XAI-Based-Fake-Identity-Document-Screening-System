package com.verilens.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Persists the OCR module output for an analysis task.
 * words_json stores per-word bounding box + confidence data as a JSON string.
 * Implemented in Phase 4.
 */
@Entity
@Table(name = "ocr_results")
public class OcrResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private AnalysisTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "words_json", columnDefinition = "TEXT")
    private String wordsJson;

    @Column(name = "overall_confidence", precision = 5, scale = 4)
    private BigDecimal overallConfidence;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public OcrResult() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public AnalysisTask getTask() { return task; }
    public void setTask(AnalysisTask task) { this.task = task; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public String getWordsJson() { return wordsJson; }
    public void setWordsJson(String wordsJson) { this.wordsJson = wordsJson; }

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
