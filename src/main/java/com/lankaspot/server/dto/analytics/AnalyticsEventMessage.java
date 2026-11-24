package com.lankaspot.server.dto.analytics;

import com.lankaspot.server.enums.AnalyticsEventType;

import java.time.Instant;
import java.util.UUID;

public record AnalyticsEventMessage(
        UUID eventId,
        Long advertisementId,
        AnalyticsEventType eventType,
        String visitorKeyHash,
        String sessionId,
        Instant occurredAt,
        Integer durationSeconds
) {
}

