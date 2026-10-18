package com.verilens.repository;

import com.verilens.entity.Document;
import com.verilens.entity.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Page<Document> findByUserId(UUID userId, Pageable pageable);

    Page<Document> findByUserIdAndStatus(UUID userId, DocumentStatus status, Pageable pageable);

    Optional<Document> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);
}
