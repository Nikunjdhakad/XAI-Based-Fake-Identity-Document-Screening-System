package com.verilens.service;

import com.verilens.dto.DocumentResponse;
import com.verilens.entity.*;
import com.verilens.repository.DocumentFileRepository;
import com.verilens.repository.DocumentRepository;
import com.verilens.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Document service — handles document upload, retrieval, and deletion.
 * All file validation is delegated to StorageService.
 */
@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentFileRepository documentFileRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentFileRepository documentFileRepository,
                           UserRepository userRepository,
                           StorageService storageService) {
        this.documentRepository = documentRepository;
        this.documentFileRepository = documentFileRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    /**
     * Upload a document and persist metadata.
     */
    @Transactional
    public DocumentResponse uploadDocument(UUID userId, MultipartFile file,
                                           String documentType, String description,
                                           MultipartFile referenceFace) throws IOException {
        // Validate the main file
        storageService.validateFile(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Store the main document file
        String storagePath = storageService.storeFile(file);
        String checksum = storageService.computeChecksum(file);

        // Create Document record
        Document document = new Document();
        document.setUser(user);
        document.setOriginalFilename(file.getOriginalFilename());
        document.setDocumentType(documentType);
        document.setDescription(description);
        document.setStatus(DocumentStatus.UPLOADED);
        document.setMimeType(file.getContentType());
        document.setFileSize(file.getSize());

        Document savedDoc = documentRepository.save(document);

        // Create DocumentFile record for the main document
        DocumentFile documentFile = new DocumentFile();
        documentFile.setDocument(savedDoc);
        documentFile.setFileType(FileType.DOCUMENT);
        documentFile.setStoragePath(storagePath);
        documentFile.setOriginalFilename(file.getOriginalFilename());
        documentFile.setMimeType(file.getContentType());
        documentFile.setFileSize(file.getSize());
        documentFile.setChecksum(checksum);
        documentFileRepository.save(documentFile);

        // If a reference face was provided, store it too
        if (referenceFace != null && !referenceFace.isEmpty()) {
            String faceStoragePath = storageService.storeFile(referenceFace);
            String faceChecksum = storageService.computeChecksum(referenceFace);

            DocumentFile faceFile = new DocumentFile();
            faceFile.setDocument(savedDoc);
            faceFile.setFileType(FileType.REFERENCE_FACE);
            faceFile.setStoragePath(faceStoragePath);
            faceFile.setOriginalFilename(referenceFace.getOriginalFilename());
            faceFile.setMimeType(referenceFace.getContentType());
            faceFile.setFileSize(referenceFace.getSize());
            faceFile.setChecksum(faceChecksum);
            documentFileRepository.save(faceFile);
        }

        return DocumentResponse.fromEntity(savedDoc);
    }

    /**
     * Get paginated documents for a user.
     */
    @Transactional(readOnly = true)
    public Page<DocumentResponse> getDocuments(UUID userId, DocumentStatus status, Pageable pageable) {
        Page<Document> documents;
        if (status != null) {
            documents = documentRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            documents = documentRepository.findByUserId(userId, pageable);
        }
        return documents.map(DocumentResponse::fromEntity);
    }

    /**
     * Get a specific document, verifying user ownership.
     */
    @Transactional(readOnly = true)
    public DocumentResponse getDocument(UUID documentId, UUID userId) {
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found or access denied"));
        return DocumentResponse.fromEntity(document);
    }

    /**
     * Delete a document and its files, verifying user ownership.
     */
    @Transactional
    public void deleteDocument(UUID documentId, UUID userId) {
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found or access denied"));

        // Delete physical files
        List<DocumentFile> files = documentFileRepository.findByDocumentId(documentId);
        for (DocumentFile file : files) {
            storageService.deleteFile(file.getStoragePath());
        }

        // Cascade delete handles document_files, analysis_tasks
        documentRepository.delete(document);
    }
}
