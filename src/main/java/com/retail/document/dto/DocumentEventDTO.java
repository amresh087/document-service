package com.retail.document.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEventDTO {
    @JsonProperty("document_id")
    private UUID documentId;

    @JsonProperty("document_name")
    private String documentName;

    @JsonProperty("document_type")
    private String documentType;

    @JsonProperty("tenant")
    private String tenant;

    @JsonProperty("transaction_type_code")
    private String transactionTypeCode;

    @JsonProperty("status")
    private String status;

    @JsonProperty("object_name")
    private String objectName;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    public enum EventType {
        DOCUMENT_CREATED,
        DOCUMENT_UPDATED,
        DOCUMENT_DELETED
    }
}
