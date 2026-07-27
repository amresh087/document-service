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
        for (JobStatusType type : values()) {
            if (type.name().equals(normalized) || type.value.equals(normalized)) {
                return type;
            }
        }

        return PENDING;
    }
}
