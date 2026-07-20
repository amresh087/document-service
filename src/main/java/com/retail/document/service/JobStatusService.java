package com.retail.document.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.retail.document.entity.JobStatus;
import com.retail.document.repository.JobStatusRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobStatusService {

    private final JobStatusRepository jobStatusRepository;

    public JobStatus createJob(UUID jobId, UUID documentId, String status) {
        JobStatus job = JobStatus.builder()
                .id(jobId)
                .documentId(documentId)
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return jobStatusRepository.save(job);
    }
}
