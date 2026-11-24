package com.lankaspot.server.service.kafka;

import com.lankaspot.server.dto.analytics.AnalyticsEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsProducer {
    private final KafkaTemplate<String, AnalyticsEventMessage> kafkaTemplate;

    @Value("${lankaspot.kafka.topics.analytics-events:analytics-events}")
    private String topic;

    public CompletableFuture<?> publish(AnalyticsEventMessage event) {
        log.info("Publishing analytics event eventId={} advertisementId={} eventType={} sessionId={}",
                event.eventId(), event.advertisementId(), event.eventType(), event.sessionId());
        return kafkaTemplate.send(topic, String.valueOf(event.advertisementId()), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Analytics event publish failed eventId={} advertisementId={} eventType={}",
                                event.eventId(), event.advertisementId(), event.eventType(), exception);
                    }
                });
    }
}

