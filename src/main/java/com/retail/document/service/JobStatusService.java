package com.retail.document.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.retail.document.dto.JobStatusType;
import com.retail.document.entity.JobStatus;
import com.retail.document.repository.JobStatusRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobStatusService {

    private final JobStatusRepository jobStatusRepository;

    public JobStatus createJob(UUID jobId, UUID documentId, String status) {
        JobStatusType jobStatusType = JobStatusType.fromValue(status);
        JobStatus existingJob = jobStatusRepository.findById(jobId).orElse(null);

        JobStatus job = existingJob != null ? existingJob : JobStatus.builder()
                .id(jobId)
                .build();

        job.setDocumentId(documentId);
        job.setStatus(jobStatusType.getValue());
        job.setUpdatedAt(LocalDateTime.now());
        if (existingJob == null) {
            job.setCreatedAt(LocalDateTime.now());
        }

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

    public String getStatus(UUID jobId) {
        if (jobId == null) {
            return null;
        }

        return jobStatusRepository.findById(jobId)
                .map(JobStatus::getStatus)
                .orElse(null);
    }

    public void deleteByDocumentId(UUID documentId) {
        jobStatusRepository.deleteByDocumentId(documentId);
    }
}
