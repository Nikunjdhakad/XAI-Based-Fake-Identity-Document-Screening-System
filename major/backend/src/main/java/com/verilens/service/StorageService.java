package com.verilens.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

/**
 * Storage service — handles secure file storage on disk.
 * Validates file type, MIME type, size, and prevents path traversal.
 * Generates safe internal filenames using UUIDs.
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final Path uploadDir;
    private final Set<String> allowedExtensions;
    private final Set<String> allowedMimeTypes;

    public StorageService(
            @Value("${verilens.storage.upload-dir}") String uploadDirPath,
            @Value("${verilens.storage.allowed-extensions}") String extensions,
            @Value("${verilens.storage.allowed-mime-types}") String mimeTypes) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        this.allowedExtensions = Set.of(extensions.toLowerCase().split(","));
        this.allowedMimeTypes = Set.of(mimeTypes.toLowerCase().split(","));
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadDir);
            log.info("Upload directory initialized: {}", uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadDir, e);
        }
    }

    /**
     * Validate a file before storing.
     *
     * @throws IllegalArgumentException if file fails validation
     */
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Validate extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("File must have an extension");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException(
                    "File extension '" + extension + "' is not allowed. Allowed: " + allowedExtensions);
        }

        // Validate MIME type
        String contentType = file.getContentType();
        if (contentType == null || !allowedMimeTypes.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "MIME type '" + contentType + "' is not allowed. Allowed: " + allowedMimeTypes);
        }
    }

    /**
     * Store a file securely. Returns the relative storage path.
     * Uses UUID-based filenames to prevent path traversal and filename collisions.
     */
    public String storeFile(MultipartFile file) throws IOException {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();

        // Generate safe internal filename
        String safeFilename = UUID.randomUUID().toString() + "." + extension;
        Path targetPath = uploadDir.resolve(safeFilename).normalize();

        // Verify the target is within the upload directory (path traversal protection)
        if (!targetPath.startsWith(uploadDir)) {
            throw new SecurityException("Path traversal attempt detected");
        }

        // Copy file to storage
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.debug("Stored file: {} -> {}", originalFilename, safeFilename);
        return safeFilename;
    }

    /**
     * Compute SHA-256 checksum for a file.
     */
    public String computeChecksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.warn("Failed to compute checksum: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Delete a file from storage.
     */
    public void deleteFile(String storagePath) {
        try {
            Path filePath = uploadDir.resolve(storagePath).normalize();
            if (!filePath.startsWith(uploadDir)) {
                throw new SecurityException("Path traversal attempt detected");
            }
            Files.deleteIfExists(filePath);
            log.debug("Deleted file: {}", storagePath);
        } catch (IOException e) {
            log.error("Failed to delete file {}: {}", storagePath, e.getMessage());
        }
    }

    /**
     * Resolve the full path of a stored file.
     */
    public Path resolveFile(String storagePath) {
        Path filePath = uploadDir.resolve(storagePath).normalize();
        if (!filePath.startsWith(uploadDir)) {
            throw new SecurityException("Path traversal attempt detected");
        }
        return filePath;
    }
}
