package com.lankaspot.server.service.kafka;

import com.lankaspot.server.event.model.EmailSending.EmailEvent;
import com.lankaspot.server.event.model.EmailSending.EmailEventBody;
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
public class EmailSendingProducer {

    private final KafkaTemplate<String, EmailEvent> kafkaTemplate;

    @Value("${lankaspot.kafka.topics.email-sending}")
    private String topic;

    public CompletableFuture<SendResult<String, EmailEvent>> publish(
            EmailEventBody body
    ) {
        EmailEvent event = new EmailEvent(
                UUID.randomUUID(),
                Instant.now(),
                body
        );

        // Keep events for the same advertisement on the same Kafka partition.
        String key = String.valueOf(body.advertisementRef());

        log.info(
                "Publishing email event. eventId={}, eventType={}, advertisementRef={}",
                event.id(),
                body.eventType(),
                body.advertisementRef()
        );

        return kafkaTemplate.send(topic, key, event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error(
                                "Failed to publish email event. eventId={}, eventType={}, advertisementRef={}",
                                event.id(),
                                body.eventType(),
                                body.advertisementRef(),
                                exception
                        );
                    } else {
                        log.info(
                                "Email event published. eventId={}, topic={}, partition={}, offset={}",
                                event.id(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()
                        );
                    }
                });
    }
}