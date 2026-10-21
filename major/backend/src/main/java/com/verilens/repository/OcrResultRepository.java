package com.verilens.repository;

import com.verilens.entity.OcrResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OcrResultRepository extends JpaRepository<OcrResult, UUID> {
    Optional<OcrResult> findByTaskId(UUID taskId);
    Optional<OcrResult> findTopByDocumentIdOrderByCreatedAtDesc(UUID documentId);
}
