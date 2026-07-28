package com.retail.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.retail.document.dto.DocumentRequest;
import com.retail.document.dto.DocumentResponse;
import com.retail.document.entity.DocumentRecord;
import com.retail.document.kafka.DocumentEventProducer;
import com.retail.document.repository.DocumentRepository;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentStorageService documentStorageService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentEventProducer documentEventProducer;

    @Mock
    private JobStatusService jobStatusService;

    @Mock
    private TransformationJobService transformationJobService;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void getAllWithMappingDocFilterReturnsOnlySupportedMappingTypes() {
        DocumentRecord xmlDocument = DocumentRecord.builder()
                .id(UUID.randomUUID())
                .name("mapping.xml")
                .type("XML")
                .tenant("tenant-a")
                .mappingType("mapping-xslt-templet-xml")
                .status("Indexed")
                .contentType("application/xml")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(documentRepository.findByMappingTypeIn(List.of(
                "mapping-xslt-templet-xml",
                "idoc-output-sample"))).thenReturn(List.of(xmlDocument));

        List<DocumentResponse> results = documentService.getAll("mappingdoc");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMappingType()).isEqualTo("mapping-xslt-templet-xml");
        assertThat(results.get(0).getType()).isEqualTo("XML");
    }

    @Test
    void getAllWithEdiToXmlFilterExcludesMappingDocuments() {
        DocumentRecord ediDocument = DocumentRecord.builder()
                .id(UUID.randomUUID())
                .name("orders.edi.txt")
                .type("TXT")
                .tenant("tenant-a")
                .transactionTypeCode("850")
                .status("Indexed")
                .contentType("text/plain")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        DocumentRecord mappingDocument = DocumentRecord.builder()
                .id(UUID.randomUUID())
                .name("merged-xslt-templet.xml")
                .type("XML")
                .tenant("tenant-a")
                .mappingType("mapping-xslt-templet-xml")
                .status("Indexed")
                .contentType("application/xml")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(documentRepository.findAll()).thenReturn(List.of(ediDocument, mappingDocument));

        List<DocumentResponse> results = documentService.getAll("edi-to-xml");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("orders.edi.txt");
        assertThat(results.get(0).getType()).isEqualTo("TXT");
    }

    @Test
    void createWithFileAllowsUploadWhenSameFileNameIsUsedForDifferentTenant() throws Exception {
        DocumentRequest request = new DocumentRequest();
        request.setName("mapping.xml");
        request.setType("XML");
        request.setTenant("tenant-b");
        request.setTransactionTypeCode("850");

        MultipartFile file = new MockMultipartFile("file", "mapping.xml", "application/xml",
                "<root />".getBytes(StandardCharsets.UTF_8));

        when(documentRepository.findByNameIgnoreCase("mapping.xml"))
                .thenReturn(Optional.of(DocumentRecord.builder()
                        .id(UUID.randomUUID())
                        .name("mapping.xml")
                        .tenant("tenant-a")
                        .build()));
        when(documentRepository.save(any(DocumentRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = documentService.createWithFile(request, file);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("mapping.xml");
        assertThat(response.getTenant()).isEqualTo("tenant-b");
        verify(documentRepository).save(any(DocumentRecord.class));
    }

    @Test
    void createWithFileAllowsTxtUploadsToBypassDuplicateCheck() throws Exception {
        DocumentRequest request = new DocumentRequest();
        request.setName("notes.txt");
        request.setType("TXT");
        request.setTenant("tenant-a");
        request.setTransactionTypeCode("txn-001");

        MultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain",
                "hello world".getBytes(StandardCharsets.UTF_8));

        when(documentRepository.save(any(DocumentRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = documentService.createWithFile(request, file);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("notes.txt");
        assertThat(response.getContentType()).isEqualTo("text/plain");
        verify(documentRepository).save(any(DocumentRecord.class));
    }
}
