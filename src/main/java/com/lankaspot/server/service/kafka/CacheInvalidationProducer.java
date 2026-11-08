
package com.lankaspot.server.service.kafka;

import com.lankaspot.server.event.model.cacheInvalidation.CacheInvalidationBody;
import com.lankaspot.server.event.model.cacheInvalidation.CacheInvalidationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationProducer {

    private final KafkaTemplate<String, CacheInvalidationEvent> kafkaTemplate;

    @Value("${lankaspot.kafka.topics.cache-invalidation}")
    private String topic;

    public CompletableFuture<SendResult<String, CacheInvalidationEvent>> publish(
            CacheInvalidationBody body
    ) {
        CacheInvalidationEvent event = new CacheInvalidationEvent(
                UUID.randomUUID(),
                Instant.now(),
                body
        );

        // Keep events for the same quote on the same Kafka partition.
        String key = String.valueOf(body.quoteId());

        log.info(
                "Publishing cache invalidation event. eventId={}, eventType={}, quoteId={}, cacheVersion={}",
                event.id(),
                body.eventType(),
                body.quoteId(),
                body.cacheVersion()
        );

        return kafkaTemplate.send(topic, key, event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error(
                                "Failed to publish cache invalidation event. eventId={}, quoteId={}",
                                event.id(),
                                body.quoteId(),
                                exception
                        );
                    } else {
                        log.info(
                                "Cache invalidation event published. eventId={}, topic={}, partition={}, offset={}",
                                event.id(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()
                        );
                    }
                });
    }
}
