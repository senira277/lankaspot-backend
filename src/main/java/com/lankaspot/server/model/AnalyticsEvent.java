package com.lankaspot.server.model;

import com.lankaspot.server.enums.AnalyticsEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analytics_events")
@Getter
@Setter
@NoArgsConstructor
public class AnalyticsEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "advertisement_id", nullable = false)
    private Long advertisementId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 48)
    private AnalyticsEventType eventType;

    @Column(name = "visitor_key_hash", nullable = false, length = 64)
    private String visitorKeyHash;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;
}

