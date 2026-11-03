package com.verilens.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Persists a generated screening report.
 * The physical PDF file is stored on disk via StorageService.
 * Implemented in Phase 12.
 */
@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private AnalysisTask task;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(nullable = false, length = 10)
    private String format = "PDF";

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) generatedAt = Instant.now();
    }

    public Report() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

    public AnalysisTask getTask() { return task; }
    public void setTask(AnalysisTask task) { this.task = task; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
