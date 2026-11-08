package com.lankaspot.server.event.listener;

import com.lankaspot.server.event.consumer.NotificationConsumer;
import com.lankaspot.server.event.model.notification.NotificationEvent;
import com.lankaspot.server.service.kafka.DeadLetterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationListener {
    private final DeadLetterService deadLetterService;
    private final NotificationConsumer notificationConsumer;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 5000),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
            topics = "notification",
            groupId = "lankaspot-notification"
    )
    public void consume(NotificationEvent event) {
        try {
            log.info("Received Notification Event: {}", event);
            notificationConsumer.consume(event);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @DltHandler
    public void handleDlt(ConsumerRecord<?, ?> record) {
        deadLetterService.save(record);
    }


}
