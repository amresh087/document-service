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

    public TransformationJob createJob(UUID jobId, UUID documentId, String jobName, String payload) {
        TransformationJob job = TransformationJob.builder()
                .id(jobId)
                .documentId(documentId)
                .jobName(jobName)
                .status(JobStatusType.SUBMITTED.getValue())
                .payload(payload)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return transformationJobRepository.save(job);
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

    public TransformationJob updateStatus(UUID id, String status) {
        TransformationJob job = transformationJobRepository.findById(id).orElse(null);
        if (job == null) {
            return null;
        }
        job.setStatus(JobStatusType.fromValue(status).getValue());
        job.setUpdatedAt(LocalDateTime.now());
        return transformationJobRepository.save(job);
    }
}
