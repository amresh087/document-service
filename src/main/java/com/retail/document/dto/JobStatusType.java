package com.retail.document.dto;

public enum JobStatusType {
    SUBMITTED("SUBMITTED"),
    EDI_TEXT_TO_EDI_XML("EDI_TEXT_TO_EDI_XML"),
    EDI_XML_TO_IDOC_XML("EDI_XML_TO_IDOC_XML"),
    PROCESSING("PROCESSING"),
    COMPLETED("COMPLETED"),
    PENDING("PENDING"),
    CANCELLED("CANCELLED"),
    FAILED("FAILED");

    private final String value;

    JobStatusType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static JobStatusType fromValue(String status) {
        if (status == null || status.isBlank()) {
            return SUBMITTED;
        }

        String normalized = status.trim().toUpperCase();
        if (normalized.contains("COMPLETED") || normalized.contains("SUCCESS") || normalized.contains("DONE")) {
            return COMPLETED;
        }
        if (normalized.contains("FAILED") || normalized.contains("ERROR") || normalized.contains("EXCEPTION")) {
            return FAILED;
        }
        if (normalized.contains("EDI_TEXT_TO_EDI_XML") || normalized.contains("EDI XML")) {
            return EDI_TEXT_TO_EDI_XML;
        }
        if (normalized.contains("EDI_XML_TO_IDOC_XML") || normalized.contains("IDOC XML") || normalized.contains("IDOC")) {
            return EDI_XML_TO_IDOC_XML;
        }
        if (normalized.contains("PROCESSING") || normalized.contains("RUNNING") || normalized.contains("WORKFLOW IS STILL") || normalized.contains("STARTED")) {
            return PROCESSING;
        }
        if (normalized.contains("SUBMITTED") || normalized.contains("ACCEPTED") || normalized.contains("UPLOAD REQUEST")) {
            return SUBMITTED;
        }
        if (normalized.contains("PENDING") || normalized.contains("WAITING") || normalized.contains("QUEUE")) {
            return PENDING;
        }
        for (JobStatusType type : values()) {
            if (type.name().equals(normalized) || type.value.equals(normalized)) {
                return type;
            }
        }

        return PENDING;
    }
}
