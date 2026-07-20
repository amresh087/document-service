package com.retail.document.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransformationJobResponse {
    private UUID id;
    private UUID documentId;
    private String jobName;
    private String status;
    private String payload;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
