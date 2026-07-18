package com.retail.document.repository;

import java.util.UUID;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.retail.document.entity.DocumentRecord;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentRecord, UUID> {
    Optional<DocumentRecord> findByName(String name);
}
