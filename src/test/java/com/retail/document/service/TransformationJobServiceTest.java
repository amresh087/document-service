package com.retail.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.retail.document.entity.JobStatus;
import com.retail.document.entity.TransformationJob;
import com.retail.document.repository.TransformationJobRepository;

@ExtendWith(MockitoExtension.class)
class TransformationJobServiceTest {

    @Mock
    private TransformationJobRepository transformationJobRepository;

    @Mock
    private JobStatusService jobStatusService;

    @InjectMocks
    private TransformationJobService transformationJobService;

    @Test
    void getJobByDocumentIdReturnsLatestJobForDocument() {
        UUID documentId = UUID.randomUUID();
        TransformationJob expectedJob = TransformationJob.builder()
                .id(UUID.randomUUID())
                .documentId(documentId)
                .build();

        when(transformationJobRepository.findFirstByDocumentIdOrderByCreatedAtDesc(documentId))
                .thenReturn(Optional.of(expectedJob));

        TransformationJob actualJob = transformationJobService.getJobByDocumentId(documentId);

        assertThat(actualJob).isSameAs(expectedJob);
        verify(transformationJobRepository).findFirstByDocumentIdOrderByCreatedAtDesc(documentId);
    }

    @Test
    void getAllJobsReturnsAllTransformationJobs() {
        TransformationJob firstJob = TransformationJob.builder()
                .id(UUID.randomUUID())
                .documentId(UUID.randomUUID())
                .build();
        TransformationJob secondJob = TransformationJob.builder()
                .id(UUID.randomUUID())
                .documentId(UUID.randomUUID())
                .build();

        when(transformationJobRepository.findAll()).thenReturn(List.of(firstJob, secondJob));

        List<TransformationJob> jobs = transformationJobService.getAllJobs();

        assertThat(jobs).containsExactly(firstJob, secondJob);
        verify(transformationJobRepository).findAll();
    }

    @Test
    void updateStatusPropagatesToTransformationJobAndJobStatusRecord() {
        UUID jobId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        TransformationJob existingJob = TransformationJob.builder()
                .id(jobId)
                .documentId(documentId)
                .build();

        when(transformationJobRepository.findById(jobId)).thenReturn(Optional.of(existingJob));
        when(transformationJobRepository.save(any(TransformationJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobStatusService.updateStatus(jobId, "COMPLETED"))
                .thenReturn(JobStatus.builder().id(jobId).documentId(documentId).status("COMPLETED").build());

        TransformationJob actualJob = transformationJobService.updateStatus(jobId, "COMPLETED");

        assertThat(actualJob).isNotNull();
        verify(jobStatusService).updateStatus(jobId, "COMPLETED");
        verify(transformationJobRepository).save(existingJob);
    }
}
