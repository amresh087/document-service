package com.retail.document.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.retail.document.entity.JobStatus;

@Repository
public interface JobStatusRepository extends JpaRepository<JobStatus, UUID> {

    void deleteByDocumentId(UUID documentId);
}
