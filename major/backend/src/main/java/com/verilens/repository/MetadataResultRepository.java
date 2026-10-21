package com.verilens.repository;

import com.verilens.entity.MetadataResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetadataResultRepository extends JpaRepository<MetadataResult, UUID> {
    Optional<MetadataResult> findByTaskId(UUID taskId);
    Optional<MetadataResult> findTopByDocumentIdOrderByCreatedAtDesc(UUID documentId);
}
