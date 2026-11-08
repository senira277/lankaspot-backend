package com.lankaspot.server.event.listener;

import com.lankaspot.server.event.consumer.EmailSendingConsumer;
import com.lankaspot.server.event.model.EmailSending.EmailEvent;
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
@RequiredArgsConstructor
@Slf4j
public class EmailSendingListener {
    private final DeadLetterService deadLetterService;
    private final EmailSendingConsumer emailSendingConsumer;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 5000),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
            topics = "email-sending",
            groupId = "lankaspot-email"
    )
    public void consume(EmailEvent event) {
        try {
            log.info("Received cache Email send Event: {}", event);
            emailSendingConsumer.consume(event);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @DltHandler
    public void handleDlt(ConsumerRecord<?, ?> record) {
        deadLetterService.save(record);
    }
}
