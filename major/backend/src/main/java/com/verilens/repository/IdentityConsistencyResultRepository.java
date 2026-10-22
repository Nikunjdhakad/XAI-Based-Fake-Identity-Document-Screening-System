package com.verilens.repository;

import com.verilens.entity.IdentityConsistencyResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdentityConsistencyResultRepository extends JpaRepository<IdentityConsistencyResult, UUID> {
    Optional<IdentityConsistencyResult> findByTaskId(UUID taskId);
    Optional<IdentityConsistencyResult> findTopByDocumentIdOrderByCreatedAtDesc(UUID documentId);
}
