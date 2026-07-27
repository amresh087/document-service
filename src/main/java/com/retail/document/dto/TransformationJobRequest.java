package com.retail.document.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class TransformationJobRequest {
    private UUID id; // optional: caller can provide id
    private UUID documentId;
    private String jobName;
    private String status;
    private String payload;
}
