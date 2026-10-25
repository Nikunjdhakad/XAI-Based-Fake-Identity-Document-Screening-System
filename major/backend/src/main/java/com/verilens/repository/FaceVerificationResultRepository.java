package com.verilens.repository;

import com.verilens.entity.FaceVerificationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FaceVerificationResultRepository extends JpaRepository<FaceVerificationResult, UUID> {
    Optional<FaceVerificationResult> findByTaskId(UUID taskId);
    Optional<FaceVerificationResult> findTopByDocumentIdOrderByCreatedAtDesc(UUID documentId);
}
