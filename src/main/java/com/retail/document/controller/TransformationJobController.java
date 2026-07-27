package com.retail.document.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.retail.document.dto.TransformationJobRequest;
import com.retail.document.dto.TransformationJobResponse;
import com.retail.document.entity.TransformationJob;
import com.retail.document.service.TransformationJobService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/documents/jobs")
@RequiredArgsConstructor
public class TransformationJobController {

    private final TransformationJobService transformationJobService;

    @PostMapping
    public ResponseEntity<TransformationJobResponse> create(@RequestBody TransformationJobRequest request) {
        UUID jobId = request.getId() != null ? request.getId() : UUID.randomUUID();
        TransformationJob job = transformationJobService.createJob(jobId, request.getDocumentId(), request.getJobName(), request.getPayload());

        return ResponseEntity.created(URI.create(String.format("/api/transformation-jobs/%s", job.getId().toString()))).body(toResponse(job));
    }

    @GetMapping
    public ResponseEntity<java.util.List<TransformationJobResponse>> getAll() {
        return ResponseEntity.ok(transformationJobService.getAllJobs().stream().map(this::toResponse).toList());
    }

    @GetMapping("/document/{documentId}")
    public ResponseEntity<TransformationJobResponse> getByDocumentId(@PathVariable("documentId") UUID documentId) {
        TransformationJob job = transformationJobService.getJobByDocumentId(documentId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toResponse(job));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransformationJobResponse> updateStatus(@PathVariable("id") UUID id, @RequestBody TransformationJobRequest request) {
        String status = request.getStatus() != null
                ? request.getStatus()
                : request.getPayload() != null ? request.getPayload() : request.getJobName();

        TransformationJob job = transformationJobService.updateStatus(id, status);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toResponse(job));
    }

    private TransformationJobResponse toResponse(TransformationJob job) {
        String latestStatus = transformationJobService.getLatestStatus(job.getId());
        return TransformationJobResponse.builder()
                .id(job.getId())
                .jobId(job.getJobId())
                .documentId(job.getDocumentId())
                .jobName(job.getJobName())
                .status(latestStatus)
                .payload(job.getPayload())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
