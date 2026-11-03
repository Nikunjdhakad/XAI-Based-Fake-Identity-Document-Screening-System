package com.verilens.service;

import com.verilens.dto.ReportResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verilens.entity.*;
import com.verilens.repository.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Report generation service ??? Phase 12.
 *
 * Generates a PDF screening report after analysis completes.
 * The report includes:
 *  - Document metadata
 *  - Risk assessment (score, level, explanations)
 *  - Per-module summaries
 *  - Suspicious regions summary
 *  - Disclaimer
 *
 * Uses Apache PDFBox (Apache 2.0 license) for PDF generation.
 * Reports are stored on disk alongside uploaded documents.
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ReportRepository reportRepository;
    private final DocumentRepository documentRepository;
    private final AnalysisTaskRepository taskRepository;
    private final RiskAssessmentRepository riskRepository;
    private final OcrResultRepository ocrRepository;
    private final MetadataResultRepository metadataRepository;
    private final IdentityConsistencyResultRepository identityRepository;
    private final ForensicResultRepository forensicsRepository;
    private final FaceVerificationResultRepository faceRepository;
    private final AiManipulationResultRepository aiManipulationRepository;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;
    private final String reportDir;

    public ReportService(
            ReportRepository reportRepository,
            DocumentRepository documentRepository,
            AnalysisTaskRepository taskRepository,
            RiskAssessmentRepository riskRepository,
            OcrResultRepository ocrRepository,
            MetadataResultRepository metadataRepository,
            IdentityConsistencyResultRepository identityRepository,
            ForensicResultRepository forensicsRepository,
            FaceVerificationResultRepository faceRepository,
            AiManipulationResultRepository aiManipulationRepository,
            StorageService storageService,
            ObjectMapper objectMapper,
            @Value("${verilens.storage.upload-dir:./uploads}") String uploadDir) {
        this.reportRepository = reportRepository;
        this.documentRepository = documentRepository;
        this.taskRepository = taskRepository;
        this.riskRepository = riskRepository;
        this.ocrRepository = ocrRepository;
        this.metadataRepository = metadataRepository;
        this.identityRepository = identityRepository;
        this.forensicsRepository = forensicsRepository;
        this.faceRepository = faceRepository;
        this.aiManipulationRepository = aiManipulationRepository;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
        this.reportDir = Paths.get(uploadDir, "reports").toString();
    }

    /**
     * Generate a PDF screening report for a completed analysis.
     * Called from AnalysisService after evidence fusion.
     */
    @Transactional
    public void generateReport(UUID taskId, UUID documentId) {
        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found for report"));

            AnalysisTask task = taskRepository.findById(taskId).orElse(null);

            // Gather all module results
            Optional<RiskAssessment> riskOpt = riskRepository.findByTaskId(taskId);
            Optional<OcrResult> ocrOpt = ocrRepository.findByTaskId(taskId);
            Optional<MetadataResult> metaOpt = metadataRepository.findByTaskId(taskId);
            Optional<IdentityConsistencyResult> identOpt = identityRepository.findByTaskId(taskId);
            Optional<ForensicResult> forensicsOpt = forensicsRepository.findByTaskId(taskId);
            Optional<FaceVerificationResult> faceOpt = faceRepository.findByTaskId(taskId);
            Optional<AiManipulationResult> aiOpt = aiManipulationRepository.findByTaskId(taskId);

            // Generate PDF bytes
            byte[] pdfBytes = buildPdf(document, task, riskOpt, ocrOpt, metaOpt, identOpt, forensicsOpt, faceOpt, aiOpt);

            // Store on disk
            String filename = "report_" + documentId + "_" + System.currentTimeMillis() + ".pdf";
            Path reportPath = Paths.get(reportDir);
            Files.createDirectories(reportPath);
            Path fullPath = reportPath.resolve(filename);
            Files.write(fullPath, pdfBytes);

            // Persist report record
            Report report = new Report();
            report.setDocument(document);
            report.setTask(task);
            report.setStoragePath(filename);
            report.setFormat("PDF");
            report.setFileSize((long) pdfBytes.length);
            reportRepository.save(report);

            log.info("Report generated for document {} ??? {} bytes, stored at {}", documentId, pdfBytes.length, filename);

        } catch (Exception e) {
            log.error("Failed to generate report for document {}: {}", documentId, e.getMessage(), e);
        }
    }

    /**
     * Get report metadata for a document.
     */
    @Transactional(readOnly = true)
    public ReportResponse getReport(UUID documentId, UUID userId) {
        documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found or access denied"));

        Report report = reportRepository
                .findTopByDocumentIdOrderByGeneratedAtDesc(documentId)
                .orElseThrow(() -> new RuntimeException("No report found for this document"));

        return ReportResponse.fromEntity(report);
    }

    /**
     * Get the raw PDF bytes for download.
     */
    @Transactional(readOnly = true)
    public byte[] downloadReport(UUID documentId, UUID userId) {
        documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found or access denied"));

        Report report = reportRepository
                .findTopByDocumentIdOrderByGeneratedAtDesc(documentId)
                .orElseThrow(() -> new RuntimeException("No report found for this document"));

        try {
            Path fullPath = Paths.get(reportDir).resolve(report.getStoragePath());
            return Files.readAllBytes(fullPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not read report file: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // PDF building
    // -----------------------------------------------------------------------

    private byte[] buildPdf(
            Document document,
            AnalysisTask task,
            Optional<RiskAssessment> riskOpt,
            Optional<OcrResult> ocrOpt,
            Optional<MetadataResult> metaOpt,
            Optional<IdentityConsistencyResult> identOpt,
            Optional<ForensicResult> forensicsOpt,
            Optional<FaceVerificationResult> faceOpt,
            Optional<AiManipulationResult> aiOpt) throws IOException {

        try (PDDocument pdf = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            pdf.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(pdf, page)) {
                float margin = 50;
                float y = PDRectangle.A4.getHeight() - margin;
                float width = PDRectangle.A4.getWidth() - 2 * margin;

                var helvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                var helveticaBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                var helveticaOblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

                // ?????? Header ??????
                cs.beginText();
                cs.setFont(helveticaBold, 20);
                cs.newLineAtOffset(margin, y);
                cs.showText("VeriLens Screening Report");
                cs.endText();
                y -= 30;

                cs.beginText();
                cs.setFont(helveticaOblique, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Evidence-first document screening ??? risk assessment system");
                cs.endText();
                y -= 25;

                // Line separator
                cs.setLineWidth(1);
                cs.moveTo(margin, y);
                cs.lineTo(margin + width, y);
                cs.stroke();
                y -= 25;

                // ?????? Document metadata ??????
                y = writeSectionHeader(cs, helveticaBold, "Document Information", margin, y);
                y = writeRow(cs, helvetica, "Filename", document.getOriginalFilename(), margin, y, width);
                y = writeRow(cs, helvetica, "Type", document.getDocumentType(), margin, y, width);
                y = writeRow(cs, helvetica, "MIME Type", document.getMimeType(), margin, y, width);
                y = writeRow(cs, helvetica, "File Size", formatFileSize(document.getFileSize()), margin, y, width);
                y = writeRow(cs, helvetica, "Uploaded", formatDate(document.getCreatedAt()), margin, y, width);
                y = writeRow(cs, helvetica, "Status", document.getStatus().name(), margin, y, width);
                if (document.getRiskLevel() != null) {
                    y = writeRow(cs, helvetica, "Risk Level", document.getRiskLevel(), margin, y, width);
                }
                y -= 10;

                // ?????? Risk Assessment ??????
                if (riskOpt.isPresent()) {
                    RiskAssessment risk = riskOpt.get();
                    y = writeSectionHeader(cs, helveticaBold, "Risk Assessment", margin, y);
                    y = writeRow(cs, helvetica, "Risk Score", (risk.getScore().doubleValue() * 100) + "/100 (v" + risk.getScoringVersion() + ")", margin, y, width);
                    y = writeRow(cs, helvetica, "Risk Level", risk.getLevel(), margin, y, width);
                    y -= 5;

                    // Explanations
                    List<String> explanations = parseStringArray(risk.getExplanationJson());
                    if (!explanations.isEmpty()) {
                        y = writeSubHeader(cs, helveticaBold, "Explanations", margin, y);
                        for (String exp : explanations) {
                            y = writeWrappedText(cs, helvetica, "??? " + exp, margin, y, width, 11);
                        }
                        y -= 5;
                    }

                    // Module contributions
                    Map<String, Object> contributions = parseObject(risk.getModuleContributionsJson());
                    if (!contributions.isEmpty()) {
                        y = writeSubHeader(cs, helveticaBold, "Module Contributions", margin, y);
                        for (Map.Entry<String, Object> entry : contributions.entrySet()) {
                            String val = entry.getValue() == null ? "N/A" :
                                    entry.getValue() instanceof Number ?
                                            String.format("%.2f", ((Number) entry.getValue()).doubleValue()) :
                                            String.valueOf(entry.getValue());
                            y = writeRow(cs, helvetica, entry.getKey(), val, margin, y, width);
                        }
                    }
                    y -= 10;
                }

                // ?????? Module Summaries ??????
                y = writeSectionHeader(cs, helveticaBold, "Module Summaries", margin, y);

                if (ocrOpt.isPresent()) {
                    OcrResult ocr = ocrOpt.get();
                    y = writeSubHeader(cs, helveticaBold, "OCR", margin, y);
                    y = writeRow(cs, helvetica, "Status", ocr.getStatus(), margin, y, width);
                    y = writeRow(cs, helvetica, "Pages", String.valueOf(ocr.getPageCount()), margin, y, width);
                    if (ocr.getOverallConfidence() != null) {
                        y = writeRow(cs, helvetica, "Confidence", (ocr.getOverallConfidence().doubleValue() * 100) + "%", margin, y, width);
                    }
                    y -= 5;
                }

                if (forensicsOpt.isPresent()) {
                    ForensicResult f = forensicsOpt.get();
                    y = writeSubHeader(cs, helveticaBold, "Image Forensics", margin, y);
                    y = writeRow(cs, helvetica, "Status", f.getStatus(), margin, y, width);
                    if (f.getRiskScore() != null) {
                        y = writeRow(cs, helvetica, "Risk Score", (f.getRiskScore().doubleValue() * 100) + "/100", margin, y, width);
                    }
                    if (f.getElaMeanError() != null) {
                        y = writeRow(cs, helvetica, "ELA Mean Error", String.format("%.4f", f.getElaMeanError().doubleValue()), margin, y, width);
                    }
                    y -= 5;
                }

                if (metaOpt.isPresent()) {
                    MetadataResult m = metaOpt.get();
                    y = writeSubHeader(cs, helveticaBold, "Metadata Analysis", margin, y);
                    y = writeRow(cs, helvetica, "Status", m.getStatus(), margin, y, width);
                    if (m.getEvidenceScore() != null) {
                        y = writeRow(cs, helvetica, "Evidence Score", (m.getEvidenceScore().doubleValue() * 100) + "/100", margin, y, width);
                    }
                    int anomalyCount = parseArray(m.getAnomaliesJson()).size();
                    y = writeRow(cs, helvetica, "Anomalies Found", String.valueOf(anomalyCount), margin, y, width);
                    y -= 5;
                }

                if (identOpt.isPresent()) {
                    IdentityConsistencyResult i = identOpt.get();
                    y = writeSubHeader(cs, helveticaBold, "Identity Consistency", margin, y);
                    y = writeRow(cs, helvetica, "Status", i.getStatus(), margin, y, width);
                    if (i.getConsistent() != null) {
                        y = writeRow(cs, helvetica, "Consistent", i.getConsistent() ? "Yes" : "No", margin, y, width);
                    }
                    y = writeRow(cs, helvetica, "Failed (H/M/L)",
                            (i.getFailedHigh() != null ? i.getFailedHigh() : 0) + "/" +
                            (i.getFailedMedium() != null ? i.getFailedMedium() : 0) + "/" +
                            (i.getFailedLow() != null ? i.getFailedLow() : 0), margin, y, width);
                    y -= 5;
                }

                if (faceOpt.isPresent()) {
                    FaceVerificationResult f = faceOpt.get();
                    y = writeSubHeader(cs, helveticaBold, "Face Verification", margin, y);
                    y = writeRow(cs, helvetica, "Status", f.getStatus(), margin, y, width);
                    if (f.getSimilarity() != null) {
                        y = writeRow(cs, helvetica, "Similarity", (f.getSimilarity().doubleValue() * 100) + "%", margin, y, width);
                    }
                    y -= 5;
                }

                if (aiOpt.isPresent()) {
                    AiManipulationResult a = aiOpt.get();
                    y = writeSubHeader(cs, helveticaBold, "AI Manipulation Screening", margin, y);
                    y = writeRow(cs, helvetica, "Status", a.getStatus(), margin, y, width);
                    if (a.getRiskScore() != null) {
                        y = writeRow(cs, helvetica, "Risk Score", (a.getRiskScore().doubleValue() * 100) + "/100", margin, y, width);
                    }
                    if (a.getManipulationType() != null) {
                        y = writeRow(cs, helvetica, "Manipulation Type", a.getManipulationType(), margin, y, width);
                    }
                    y -= 10;
                }

                // ?????? Disclaimer ??????
                // Note: if y is too low, content may overflow the page.
                // For a simple report this is acceptable ??? the content is compact.
                y -= 10;
                cs.setLineWidth(0.5f);
                cs.moveTo(margin, y);
                cs.lineTo(margin + width, y);
                cs.stroke();
                y -= 20;

                y = writeSubHeader(cs, helveticaBold, "Disclaimer", margin, y);
                y = writeWrappedText(cs, helveticaOblique, 9,
                        "VeriLens provides risk assessments ??? not legal determinations. " +
                        "Results indicate \"potential manipulation\" and \"elevated risk\", " +
                        "not definitive proof of authenticity or forgery. " +
                        "AI detection is not 100% reliable. Metadata can be missing or spoofed. " +
                        "OCR may contain errors. Face verification is a similarity signal. " +
                        "Legal authenticity is outside the scope of this system.",
                        margin, y, width, 11);

                // Footer
                y = 40;
                cs.beginText();
                cs.setFont(helveticaOblique, 8);
                cs.newLineAtOffset(margin, y);
                cs.showText("Generated by VeriLens ??? " + formatDate(Instant.now()) + " ??? Page 1 of 1");
                cs.endText();
            }

            pdf.save(out);
            return out.toByteArray();
        }
    }

    // -----------------------------------------------------------------------
    // PDF helper methods
    // -----------------------------------------------------------------------

    private float writeSectionHeader(PDPageContentStream cs, PDType1Font font, String text,
                                      float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, 14);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y - 22;
    }

    private float writeSubHeader(PDPageContentStream cs, PDType1Font font, String text,
                                   float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, 12);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y - 17;
    }

    private float writeRow(PDPageContentStream cs, PDType1Font font,
                            String label, String value, float x, float y, float width) throws IOException {
        cs.beginText();
        cs.setFont(font, 10);
        cs.newLineAtOffset(x, y);
        cs.showText(label + ": " + value);
        cs.endText();
        return y - 15;
    }

    private float writeWrappedText(PDPageContentStream cs, PDType1Font font, float fontSize,
                                    String text, float x, float y, float width, float leading) throws IOException {
        // Simple word-wrap
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            float textWidth = font.getStringWidth(test) / 1000 * fontSize;
            if (textWidth > width && line.length() > 0) {
                cs.beginText();
                cs.setFont(font, fontSize);
                cs.newLineAtOffset(x, y);
                cs.showText(line.toString());
                cs.endText();
                y -= leading;
                line = new StringBuilder(word);
            } else {
                if (line.length() > 0) line.append(" ");
                line.append(word);
            }
        }
        if (line.length() > 0) {
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(x, y);
            cs.showText(line.toString());
            cs.endText();
            y -= leading;
        }
        return y;
    }

    // -----------------------------------------------------------------------
    // JSON parsing helpers
    // -----------------------------------------------------------------------

    private List<String> parseStringArray(String json) {
        if (json == null) return List.of();
        try {
            List<Object> list = objectMapper.readValue(json, List.class);
            return list.stream().map(Object::toString).toList();
        } catch (Exception) {
            return List.of();
        }
    }

    private Map<String, Object> parseObject(String json) {
        if (json == null) return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception) {
            return Map.of();
        }
    }

    private List<Object> parseArray(String json) {
        if (json == null) return List.of();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception) {
            return List.of();
        }
    }

    // -----------------------------------------------------------------------
    // Formatting helpers
    // -----------------------------------------------------------------------

    private String formatDate(Instant instant) {
        if (instant == null) return "???";
        return DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }
}
