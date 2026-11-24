package com.lankaspot.server.event.listener;

import com.lankaspot.server.dto.analytics.AnalyticsEventMessage;
import com.lankaspot.server.service.AnalyticsAggregationService;
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
public class AnalyticsListener {
    private final AnalyticsAggregationService aggregationService;
    private final DeadLetterService deadLetterService;

    @RetryableTopic(attempts = "4", backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 5000),
            dltStrategy = DltStrategy.FAIL_ON_ERROR)
    @KafkaListener(topics = "${lankaspot.kafka.topics.analytics-events:analytics-events}",
            groupId = "lankaspot-analytics")
    public void consume(AnalyticsEventMessage event) {
        aggregationService.process(event);
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<?, ?> record) {
        log.error("Analytics event reached DLT topic={} partition={} offset={}",
                record.topic(), record.partition(), record.offset());
        deadLetterService.save(record);
    }
}

