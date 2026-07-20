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
        JobStatusType jobStatusType = JobStatusType.fromValue(status);
        JobStatus job = JobStatus.builder()
                .id(jobId)
                .documentId(documentId)
                .status(jobStatusType.getValue())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return jobStatusRepository.save(job);
    }

    public JobStatus updateStatus(UUID jobId, String status) {
        JobStatus job = jobStatusRepository.findById(jobId).orElse(null);
        if (job == null) {
            return null;
        }

        job.setStatus(JobStatusType.fromValue(status).getValue());
        job.setUpdatedAt(LocalDateTime.now());
        return jobStatusRepository.save(job);
    }
}
