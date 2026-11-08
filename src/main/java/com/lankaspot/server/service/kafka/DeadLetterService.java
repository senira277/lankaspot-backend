package com.lankaspot.server.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lankaspot.server.enums.DeadLetterStatus;
import com.lankaspot.server.model.DeadLetterEvent;
import com.lankaspot.server.repository.DeadLetterEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterService {

    private final DeadLetterEventRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void save(ConsumerRecord<?, ?> record) {

        String consumerGroup = getStringHeader(
                record, KafkaHeaders.DLT_ORIGINAL_CONSUMER_GROUP
        );

        String exceptionClass = getStringHeader(
                record, KafkaHeaders.DLT_EXCEPTION_FQCN
        );

        String exceptionMessage = getStringHeader(
                record, KafkaHeaders.DLT_EXCEPTION_MESSAGE
        );

        String originalTopic = getStringHeader(record, KafkaHeaders.DLT_ORIGINAL_TOPIC);
        Integer partition = getIntegerHeader(record, KafkaHeaders.DLT_ORIGINAL_PARTITION);
        Long offset = getLongHeader(record, KafkaHeaders.DLT_ORIGINAL_OFFSET);

        if (originalTopic == null || partition == null || offset == null) {
            log.warn("DLT record has no original metadata, using DLT record's own. DltTopic={}", record.topic());
            originalTopic = originalTopic != null ? originalTopic : record.topic();
            partition = partition != null ? partition : record.partition();
            offset = offset != null ? offset : record.offset();
        }

        String payload;

        try {
            payload = objectMapper.writeValueAsString(record.value());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize dead-letter payload", e
            );
        }

        DeadLetterEvent event = DeadLetterEvent.builder()
                .eventId(extractEventId(record.value()))
                .originalTopic(originalTopic)
                .consumerGroup(consumerGroup)
                .partitionNumber(partition)
                .offsetNumber(offset)
                .payload(payload)
                .exceptionClass(exceptionClass)
                .exceptionMessage(exceptionMessage)
                .failedAt(Instant.now())
                .status(DeadLetterStatus.PENDING)
                .build();

        repository.save(event);

        log.error(
                "Kafka event moved to dead-letter storage. Topic={}, Partition={}, Offset={}",
                originalTopic,
                partition,
                offset
        );
    }

    private String getStringHeader(
            ConsumerRecord<?, ?> record,
            String headerName
    ) {
        Header header = record.headers().lastHeader(headerName);

        return header == null
                ? null
                : new String(header.value(), StandardCharsets.UTF_8);
    }

    private Integer getIntegerHeader(
            ConsumerRecord<?, ?> record,
            String headerName
    ) {
        Header header = record.headers().lastHeader(headerName);

        return header == null
                ? null
                : ByteBuffer.wrap(header.value()).getInt();
    }

    private Long getLongHeader(
            ConsumerRecord<?, ?> record,
            String headerName
    ) {
        Header header = record.headers().lastHeader(headerName);

        return header == null
                ? null
                : ByteBuffer.wrap(header.value()).getLong();
    }

    private java.util.UUID extractEventId(Object payload) {
        if (payload == null) {
            return null;
        }

        try {
            return objectMapper.treeToValue(
                    objectMapper.valueToTree(payload).get("id"),
                    java.util.UUID.class
            );
        } catch (Exception e) {
            log.warn("Could not extract event ID from dead-letter payload");
            return null;
        }
    }
}