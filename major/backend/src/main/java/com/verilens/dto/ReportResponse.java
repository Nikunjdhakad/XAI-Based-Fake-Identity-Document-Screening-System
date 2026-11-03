package com.verilens.dto;

import com.verilens.entity.Report;

import java.time.Instant;
import java.util.UUID;

/**
 * Report metadata DTO — returned by GET /api/documents/{id}/report.
 */
public class ReportResponse {

    private UUID reportId;
    private UUID documentId;
    private String format;
    private Long fileSize;
    private Instant generatedAt;
    private String downloadUrl;

    public ReportResponse() {}

    public static ReportResponse fromEntity(Report r) {
        ReportResponse dto = new ReportResponse();
        dto.setReportId(r.getId());
        dto.setDocumentId(r.getDocument().getId());
        dto.setFormat(r.getFormat());
        dto.setFileSize(r.getFileSize());
        dto.setGeneratedAt(r.getGeneratedAt());
        dto.setDownloadUrl("/api/documents/" + r.getDocument().getId() + "/report/download");
        return dto;
    }

    public UUID getReportId() { return reportId; }
    public void setReportId(UUID reportId) { this.reportId = reportId; }

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
}
