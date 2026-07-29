package com.retail.document.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.retail.document.dto.DocumentEventDTO;
import com.retail.document.dto.DocumentPageResponse;
import com.retail.document.dto.DocumentRequest;
import com.retail.document.dto.DocumentResponse;
import com.retail.document.entity.DocumentRecord;
import com.retail.document.kafka.DocumentEventProducer;
import com.retail.document.repository.DocumentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentStorageService documentStorageService;
    private final DocumentRepository documentRepository;
    private final DocumentEventProducer documentEventProducer;
    private final TransformationJobService transformationJobService;

    public DocumentResponse create(DocumentRequest request) {
        DocumentRecord record = DocumentRecord.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .type(normalizeType(request.getType()))
                .tenant(request.getTenant())
                .transactionTypeCode(request.getTransactionTypeCode())
                .mappingType(request.getMappingType())
                .version(request.getVersion())
                .status(request.getStatus() != null ? request.getStatus() : "Indexed")
                .contentType(normalizeContentType(request.getContentType(), request.getName()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // Save to database
        DocumentRecord saved = documentRepository.save(record);
        
        // Publish Kafka event after successful save
        DocumentEventDTO eventDTO = DocumentEventDTO.builder()
                .documentId(saved.getId())
                .documentName(saved.getName())
                .documentType(saved.getType())
                .tenant(saved.getTenant())
                .transactionTypeCode(saved.getTransactionTypeCode())
                .mappingType(saved.getMappingType())
                .status(saved.getStatus())
                .objectName(saved.getObjectName())
                .timestamp(saved.getCreatedAt())
                .build();
        
        documentEventProducer.publishDocumentCreatedEvent(eventDTO);
        
        return toResponse(saved);
    }

    public DocumentResponse createWithFile(DocumentRequest request, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must be provided for upload");
        }

        String documentName = request.getName() != null ? request.getName().trim() : file.getOriginalFilename();
        boolean skipDuplicateCheck = isTextFile(documentName) || isTextFile(file.getOriginalFilename());
        if (!skipDuplicateCheck && documentName != null && !documentName.isBlank()) {
            String requestedTenant = request.getTenant() != null ? request.getTenant().trim() : null;
            List<DocumentRecord> existingDocuments = documentRepository.findByNameIgnoreCase(documentName);
            boolean hasSameTenantConflict = existingDocuments.stream().anyMatch(existing -> {
                boolean sameTenant = requestedTenant == null
                        ? existing.getTenant() == null
                        : requestedTenant.equalsIgnoreCase(existing.getTenant());
                return sameTenant;
            });
            if (hasSameTenantConflict) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "File already exists. You can update it.");
            }
        }

        UUID documentId = UUID.randomUUID();
        String contentType = normalizeContentType(request.getContentType(), file.getOriginalFilename());
        String objectName = buildObjectName(
                request.getTenant(),
                request.getTransactionTypeCode(),
                normalizeType(request.getType()),
                file.getOriginalFilename());

        String storageKey;
        try {
            storageKey = documentStorageService.storeFile(documentId, objectName, contentType,
                    file.getInputStream(), file.getSize());
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to store uploaded file", ex);
        }

        DocumentRecord record = DocumentRecord.builder()
                .id(documentId)
                .name(request.getName() != null ? request.getName() : file.getOriginalFilename())
                .type(normalizeType(request.getType()))
                .tenant(request.getTenant())
                .transactionTypeCode(request.getTransactionTypeCode())
                .mappingType(request.getMappingType())
                .version(request.getVersion())
                .status(request.getStatus() != null ? request.getStatus() : "Indexed")
                .contentType(contentType)
                .objectName(storageKey)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // Save to database
        DocumentRecord saved = documentRepository.save(record);
        
        // Publish Kafka event after successful save
        DocumentEventDTO eventDTO = DocumentEventDTO.builder()
                .documentId(saved.getId())
                .documentName(saved.getName())
                .documentType(saved.getType())
                .tenant(saved.getTenant())
                .transactionTypeCode(saved.getTransactionTypeCode())
                .mappingType(saved.getMappingType())
                .status(saved.getStatus())
                .objectName(saved.getObjectName())
                .timestamp(saved.getCreatedAt())
                .build();

        if (saved.getType().equalsIgnoreCase("XML") ) {
            documentEventProducer.publishDocumentCreatedEvent(eventDTO);
        } else {
            // For non-XML documents, create a transformation job and publish to the transformation topic
            java.util.UUID jobId = java.util.UUID.randomUUID();

            // persist a full transformation_job record with payload metadata and a matching job_status row
            String payload = String.format("name=%s;tenant=%s;transactionType=%s", saved.getName(), saved.getTenant(), saved.getTransactionTypeCode());
            transformationJobService.createJob(jobId, saved.getId(), "edi-transformation", payload);

            // attach job id to event and publish to the transformation topic
            eventDTO.setJobId(jobId);
            documentEventProducer.publishTransformationEvent(eventDTO);

           // saved.setEdiXml(eventDTO.getpa);
        }
        
        return toResponse(saved);
    }

    public List<DocumentResponse> getAll() {
        return getAll(null);
    }

    public DocumentPageResponse getPage(String mappingdoc, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);

        boolean shouldFilterMappingDocs = "mappingdoc".equalsIgnoreCase(mappingdoc);
        boolean shouldFilterEdiTransactions = "edi-to-xml".equalsIgnoreCase(mappingdoc);

        List<DocumentRecord> records = shouldFilterMappingDocs
                ? documentRepository.findByMappingTypeIn(List.of(
                        "mapping-xslt-templet-xml",
                        "idoc-output-sample"))
                : documentRepository.findAll();

        List<DocumentRecord> filteredRecords = shouldFilterEdiTransactions
                ? records.stream()
                        .filter(record -> record.getMappingType() == null || record.getMappingType().isBlank()
                                || !isSupportedMappingType(record.getMappingType()))
                        .toList()
                : records;

        int totalElements = filteredRecords.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);
        int fromIndex = Math.min(safePage * safeSize, totalElements);
        int toIndex = Math.min(fromIndex + safeSize, totalElements);

        List<DocumentResponse> items = filteredRecords.subList(fromIndex, toIndex).stream()
                .map(record -> toResponse(record, shouldFilterMappingDocs))
                .toList();

        return DocumentPageResponse.builder()
                .items(items)
                .page(safePage)
                .size(safeSize)
                .totalElements(totalElements)
                .totalPages(Math.max(totalPages, 1))
                .hasNext(safePage + 1 < Math.max(totalPages, 1))
                .hasPrevious(safePage > 0)
                .build();
    }

    public List<DocumentResponse> getAll(String mappingdoc) {
        boolean shouldFilterMappingDocs = "mappingdoc".equalsIgnoreCase(mappingdoc);
        boolean shouldFilterEdiTransactions = "edi-to-xml".equalsIgnoreCase(mappingdoc);

        List<DocumentRecord> records = shouldFilterMappingDocs
                ? documentRepository.findByMappingTypeIn(List.of(
                        "mapping-xslt-templet-xml",
                        "idoc-output-sample"))
                : documentRepository.findAll();

        List<DocumentRecord> filteredRecords = shouldFilterEdiTransactions
                ? records.stream()
                        .filter(record -> record.getMappingType() == null || record.getMappingType().isBlank()
                                || !isSupportedMappingType(record.getMappingType()))
                        .toList()
                : records;

        return filteredRecords.stream()
                .map(record -> toResponse(record, shouldFilterMappingDocs))
                .toList();
    }

    public DocumentResponse getById(UUID id) {
        DocumentRecord record = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
        return toResponse(record);
    }

    public DocumentResponse getById(String id) {
        UUID parsedId = parseUuid(id);
        return getById(parsedId);
    }

    public DocumentResponse update(UUID id, DocumentRequest request) {
        DocumentRecord existing = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        String updatedName = request.getName() != null ? request.getName() : existing.getName();
        String updatedType = normalizeType(request.getType() != null ? request.getType() : existing.getType());
        String updatedTenant = request.getTenant() != null ? request.getTenant() : existing.getTenant();
        String updatedVersion = request.getVersion() != null ? request.getVersion() : existing.getVersion();
        String updatedStatus = request.getStatus() != null ? request.getStatus() : existing.getStatus();
        String updatedContentType = normalizeContentType(request.getContentType(), updatedName);
        String updatedMappingType = request.getMappingType() != null ? request.getMappingType() : existing.getMappingType();

        String newObjectName = existing.getObjectName();
        if (existing.getObjectName() != null && !existing.getObjectName().isBlank()) {
            String currentFileName = existing.getObjectName().substring(existing.getObjectName().lastIndexOf('/') + 1);
            String targetFileName = updatedName == null ? currentFileName : updatedName.replaceAll("[^a-zA-Z0-9_.-]", "_");
            String targetObjectName = buildObjectName(updatedTenant, existing.getTransactionTypeCode(), updatedType, targetFileName);
            if (!existing.getObjectName().equals(targetObjectName)) {
                documentStorageService.renameFile(existing.getObjectName(), targetObjectName);
                newObjectName = targetObjectName;
            }
        }

        DocumentRecord updated = DocumentRecord.builder()
                .id(existing.getId())
                .name(updatedName)
                .type(updatedType)
                .tenant(updatedTenant)
                .transactionTypeCode(request.getTransactionTypeCode() != null ? request.getTransactionTypeCode() : existing.getTransactionTypeCode())
                .mappingType(updatedMappingType)
                .version(updatedVersion)
                .status(updatedStatus)
                .contentType(updatedContentType)
                .objectName(newObjectName)
                .createdAt(existing.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // Save updated record to database
        DocumentRecord saved = documentRepository.save(updated);
        
        // Publish Kafka event after successful update
        DocumentEventDTO eventDTO = DocumentEventDTO.builder()
                .documentId(saved.getId())
                .documentName(saved.getName())
                .documentType(saved.getType())
                .tenant(saved.getTenant())
                .transactionTypeCode(saved.getTransactionTypeCode())
                .mappingType(saved.getMappingType())
                .status(saved.getStatus())
                .objectName(saved.getObjectName())
                .timestamp(saved.getUpdatedAt())
                .build();
        
        documentEventProducer.publishDocumentUpdatedEvent(eventDTO);
        
        return toResponse(saved);
    }

    public void delete(String id) {
        UUID parsedId = parseUuid(id);
        delete(parsedId);
    }

    public void delete(UUID id) {
        DocumentRecord existing = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        if (existing.getObjectName() != null && !existing.getObjectName().isBlank()) {
            documentStorageService.deleteFile(existing.getObjectName());
        }

        documentRepository.deleteById(id);

        // Clean up matching job status rows for any transformation jobs linked to this document
        //jobStatusService.deleteByDocumentId(id);
       // transformationJobService.deleteByDocumentId(id);
        
        // Publish Kafka event after successful deletion
        DocumentEventDTO eventDTO = DocumentEventDTO.builder()
                .documentId(existing.getId())
                .documentName(existing.getName())
                .documentType(existing.getType())
                .tenant(existing.getTenant())
                .transactionTypeCode(existing.getTransactionTypeCode())
                .status(existing.getStatus())
                .objectName(existing.getObjectName())
                .timestamp(LocalDateTime.now())
                .build();
        
        documentEventProducer.publishDocumentDeletedEvent(eventDTO);
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Document id is required");
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid document id format", ex);
        }
    }

    private DocumentResponse toResponse(DocumentRecord record) {
        return toResponse(record, false);
    }

    private DocumentResponse toResponse(DocumentRecord record, boolean mappingDocMode) {
        String resolvedType = record.getType();
        if (mappingDocMode) {
            resolvedType = isSupportedMappingType(record.getMappingType()) ? "XML" : "TXT";
        }

        return DocumentResponse.builder()
                .id(record.getId())
                .name(record.getName())
                .type(resolvedType)
                .tenant(record.getTenant())
                .transactionTypeCode(record.getTransactionTypeCode())
                .version(record.getVersion())
                .status(record.getStatus())
                .contentType(record.getContentType())
                .mappingType(record.getMappingType())
                .objectName(record.getObjectName())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    private boolean isSupportedMappingType(String mappingType) {
        return "mapping-xslt-templet-xml".equalsIgnoreCase(mappingType)
                || "idoc-output-sample".equalsIgnoreCase(mappingType);
    }

    /*
    private String buildObjectName(String tenant, String transactionTypeCode, String type, String originalFileName) {
        String safeTenant = tenant == null ? "tenant" : tenant.replaceAll("[^a-zA-Z0-9_.-]", "_");
        String safeTransactionTypeCode = transactionTypeCode == null ? "transcode" : transactionTypeCode.replaceAll("[^a-zA-Z0-9_.-]", "_");
        String safeType = type == null ? "PDF" : type.replaceAll("[^a-zA-Z0-9_.-]", "_");
        String safeFileName = originalFileName == null ? "file" : originalFileName.replaceAll("[^a-zA-Z0-9_.-]", "_");
        return String.format("%s_%s_%s_%s", safeTenant, safeTransactionTypeCode, safeType, safeFileName);
    }
    */

    private String buildObjectName(String tenant,
            String transactionTypeCode,
            String type,
            String originalFileName) {

        String safeTenant = tenant == null
                ? "tenant"
                : tenant.replaceAll("[^a-zA-Z0-9_.-]", "_");

        String safeTransactionTypeCode = transactionTypeCode == null
                ? "transcode"
                : transactionTypeCode.replaceAll("[^a-zA-Z0-9_.-]", "_");

        String safeType = type == null
                ? "PDF"
                : type.replaceAll("[^a-zA-Z0-9_.-]", "_");

        String safeFileName = originalFileName == null
                ? "file"
                : originalFileName.replaceAll("[^a-zA-Z0-9_.-]", "_");

        String prefixedFileName = String.format("%s-%s-%s-%s",
                safeTenant,
                safeTransactionTypeCode,
                safeType,
                safeFileName);

        return String.format("%s/%s/%s/%s",
                safeTenant,
                safeTransactionTypeCode,
                safeType,
                prefixedFileName);
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "PDF";
        }
        return type.trim().toUpperCase();
    }

    private String normalizeContentType(String contentType, String name) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }
        if (name != null && name.toLowerCase().endsWith(".xml")) {
            return "application/xml";
        }
        if (isTextFile(name)) {
            return "text/plain";
        }
        return "application/pdf";
    }

    private boolean isTextFile(String name) {
        return name != null && !name.isBlank() && name.toLowerCase().endsWith(".txt");
    }
}
