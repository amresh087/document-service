package com.retail.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.retail.document.entity.JobStatus;
import com.retail.document.repository.JobStatusRepository;

@ExtendWith(MockitoExtension.class)
class JobStatusServiceTest {

    @Mock
    private JobStatusRepository jobStatusRepository;

    @InjectMocks
    private JobStatusService jobStatusService;

    @Test
    void createJobUpdatesExistingRecordWhenJobIdAlreadyExists() {
        UUID jobId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        JobStatus existingJob = JobStatus.builder()
                .id(jobId)
                .documentId(UUID.randomUUID())
                .status("SUBMITTED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(jobStatusRepository.findById(jobId)).thenReturn(Optional.of(existingJob));
        when(jobStatusRepository.save(any(JobStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobStatus updatedJob = jobStatusService.createJob(jobId, documentId, "COMPLETED");

        assertThat(updatedJob).isSameAs(existingJob);
        assertThat(existingJob.getDocumentId()).isEqualTo(documentId);
        assertThat(existingJob.getStatus()).isEqualTo("COMPLETED");
        verify(jobStatusRepository).save(existingJob);
    }
}
