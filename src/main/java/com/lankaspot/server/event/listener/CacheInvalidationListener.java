package com.lankaspot.server.event.listener;

import com.lankaspot.server.event.model.cacheInvalidation.CacheInvalidationEvent;
import com.lankaspot.server.event.consumer.CacheInvalidationConsumer;
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
public class CacheInvalidationListener {

    private final CacheInvalidationConsumer cacheInvalidationConsumer;
    private final DeadLetterService deadLetterService;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 5000),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
            topics = "cache-invalidation",
            groupId = "lankaspot-cache"
    )
    public void consume(CacheInvalidationEvent event) {
        log.info("Received cache invalidation Event: {}", event);
        cacheInvalidationConsumer.consume(event);
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<?, ?> record) {
        deadLetterService.save(record);
    }
}