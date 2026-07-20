package com.retail.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.retail.document.entity.TransformationJob;
import com.retail.document.repository.TransformationJobRepository;

@ExtendWith(MockitoExtension.class)
class TransformationJobServiceTest {

    @Mock
    private TransformationJobRepository transformationJobRepository;

    @InjectMocks
    private TransformationJobService transformationJobService;

    @Test
    void getJobByDocumentIdReturnsLatestJobForDocument() {
        UUID documentId = UUID.randomUUID();
        TransformationJob expectedJob = TransformationJob.builder()
                .id(UUID.randomUUID())
                .documentId(documentId)
                .status("PENDING")
                .build();

        when(transformationJobRepository.findFirstByDocumentIdOrderByCreatedAtDesc(documentId))
                .thenReturn(Optional.of(expectedJob));

        TransformationJob actualJob = transformationJobService.getJobByDocumentId(documentId);

        assertThat(actualJob).isSameAs(expectedJob);
        verify(transformationJobRepository).findFirstByDocumentIdOrderByCreatedAtDesc(documentId);
    }
}
