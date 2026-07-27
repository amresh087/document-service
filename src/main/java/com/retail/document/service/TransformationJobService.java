package com.retail.document.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.retail.document.dto.JobStatusType;
import com.retail.document.entity.TransformationJob;
import com.retail.document.repository.TransformationJobRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransformationJobService {

    private final TransformationJobRepository transformationJobRepository;
    private final JobStatusService jobStatusService;

    public TransformationJob createJob(UUID jobId, UUID documentId, String jobName, String payload) {
        TransformationJob job = TransformationJob.builder()
                .id(jobId)
                .jobId(jobId)
                .documentId(documentId)
                .jobName(jobName)
                .payload(payload)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        TransformationJob savedJob = transformationJobRepository.save(job);
        jobStatusService.createJob(savedJob.getId(), documentId, JobStatusType.SUBMITTED.getValue());

        return savedJob;
    }

    public TransformationJob getJob(UUID id) {
        return transformationJobRepository.findById(id).orElse(null);
    }

    public TransformationJob getJobByDocumentId(UUID documentId) {
        return transformationJobRepository.findFirstByDocumentIdOrderByCreatedAtDesc(documentId).orElse(null);
    }

    public List<TransformationJob> getAllJobs() {
        return transformationJobRepository.findAll();
    }

    public void deleteByDocumentId(UUID documentId) {
        transformationJobRepository.deleteByDocumentId(documentId);
    }

    public TransformationJob updateStatus(UUID id, String status) {
        TransformationJob job = transformationJobRepository.findById(id).orElse(null);
        if (job == null) {
            return null;
        }

        String normalizedStatus = JobStatusType.fromValue(status).getValue();
        job.setUpdatedAt(LocalDateTime.now());
        TransformationJob savedJob = transformationJobRepository.save(job);

        if (savedJob.getId() != null) {
            jobStatusService.updateStatus(savedJob.getId(), normalizedStatus);
        }

        return savedJob;
    }

    public String getLatestStatus(UUID jobId) {
        if (jobId == null) {
            return null;
        }

        return jobStatusService.getStatus(jobId);
    }
}
