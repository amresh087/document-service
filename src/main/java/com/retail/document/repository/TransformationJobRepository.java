package com.retail.document.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.retail.document.entity.TransformationJob;

@Repository
public interface TransformationJobRepository extends JpaRepository<TransformationJob, UUID> {

    Optional<TransformationJob> findFirstByDocumentIdOrderByCreatedAtDesc(UUID documentId);
}
