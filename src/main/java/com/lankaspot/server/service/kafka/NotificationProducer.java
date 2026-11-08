package com.lankaspot.server.service.kafka;

import com.lankaspot.server.event.model.notification.NotificationEvent;
import com.lankaspot.server.event.model.notification.NotificationEventBody;
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
public class NotificationProducer {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Value("${lankaspot.kafka.topics.notification}")
    private String topic;

    public CompletableFuture<SendResult<String, NotificationEvent>> publish(
            NotificationEventBody body
    ) {
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID(),
                Instant.now(),
                body
        );

        // Keep notifications for the same recipient on the same Kafka partition.
        String key = String.valueOf(body.notification().getRecipientId());

        log.info(
                "Publishing notification event. eventId={}, notificationType={}, recipientId={}",
                event.id(),
                body.notification().getType(),
                body.notification().getRecipientId()
        );

        return kafkaTemplate.send(topic, key, event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error(
                                "Failed to publish notification event. eventId={}, notificationType={}, recipientId={}",
                                event.id(),
                                body.notification().getType(),
                                body.notification().getRecipientId(),
                                exception
                        );
                    } else {
                        log.info(
                                "Notification event published. eventId={}, topic={}, partition={}, offset={}",
                                event.id(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()
                        );
                    }
                });
    }
}