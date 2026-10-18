package com.verilens.dto;

import com.verilens.entity.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * Document response DTO for API responses.
 */
public class DocumentResponse {

    private UUID id;
    private String filename;
    private String documentType;
    private String description;
    private String status;
    private String riskLevel;
    private long fileSize;
    private String mimeType;
    private Instant uploadedAt;
    private UUID uploadedBy;

    public DocumentResponse() {}

    public static DocumentResponse fromEntity(Document doc) {
        DocumentResponse dto = new DocumentResponse();
        dto.setId(doc.getId());
        dto.setFilename(doc.getOriginalFilename());
        dto.setDocumentType(doc.getDocumentType());
        dto.setDescription(doc.getDescription());
        dto.setStatus(doc.getStatus().name());
        dto.setRiskLevel(null); // Populated by risk engine in Phase 10
        dto.setFileSize(doc.getFileSize());
        dto.setMimeType(doc.getMimeType());
        dto.setUploadedAt(doc.getCreatedAt());
        dto.setUploadedBy(doc.getUser().getId());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }

    public UUID getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(UUID uploadedBy) { this.uploadedBy = uploadedBy; }
}
