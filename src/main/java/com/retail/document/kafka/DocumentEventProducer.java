package com.retail.document.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import com.retail.document.dto.DocumentEventDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(DocumentEventProducer.class);

    private final KafkaTemplate<String, DocumentEventDTO> kafkaTemplate;

    @Value("${kafka.topics.document-events:document-events}")
    private String documentEventsTopic;

    @Value("${kafka.topics.transformation-events:transformation-events}")
    private String transformationEventsTopic;

    public void publishDocumentEvent(DocumentEventDTO event) {
        try {
            String key = event.getDocumentId().toString();
            
            Message<DocumentEventDTO> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, documentEventsTopic)
                    .setHeader(KafkaHeaders.KEY, key)
                    .setHeader("event_type", event.getEventType())
                    .setHeader("tenant", event.getTenant())
                    .build();

            kafkaTemplate.send(message);
            
            logger.info("Published document event: eventType={}, documentId={}, tenant={}",
                    event.getEventType(), event.getDocumentId(), event.getTenant());
            
        } catch (Exception ex) {
            logger.error("Failed to publish document event for documentId: {}",
                    event.getDocumentId(), ex);
            throw new RuntimeException("Failed to publish document event", ex);
        }
    }

    public void publishDocumentCreatedEvent(DocumentEventDTO event) {
        event.setEventType(DocumentEventDTO.EventType.DOCUMENT_CREATED.name());
        publishDocumentEvent(event);
    }

    public void publishDocumentUpdatedEvent(DocumentEventDTO event) {
        event.setEventType(DocumentEventDTO.EventType.DOCUMENT_UPDATED.name());
        publishDocumentEvent(event);
    }

    public void publishDocumentDeletedEvent(DocumentEventDTO event) {
        event.setEventType(DocumentEventDTO.EventType.DOCUMENT_DELETED.name());
        publishDocumentEvent(event);
    }

    public void publishTransformationEvent(DocumentEventDTO event) {
        // mark event type for transformation requests
        event.setEventType(DocumentEventDTO.EventType.TRANSFORMATION_REQUEST.name());
        try {
            String key = event.getDocumentId().toString();

            Message<DocumentEventDTO> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, transformationEventsTopic)
                    .setHeader(KafkaHeaders.KEY, key)
                    .setHeader("event_type", event.getEventType())
                    .setHeader("tenant", event.getTenant())
                    .build();

            kafkaTemplate.send(message);

            logger.info("Published transformation event: eventType={}, documentId={}, tenant={}, jobId={}",
                    event.getEventType(), event.getDocumentId(), event.getTenant(), event.getJobId());

        } catch (Exception ex) {
            logger.error("Failed to publish transformation event for documentId: {}",
                    event.getDocumentId(), ex);
            throw new RuntimeException("Failed to publish transformation event", ex);
        }
    }
}
