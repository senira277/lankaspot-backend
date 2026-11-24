package com.lankaspot.server.service;

import com.lankaspot.server.dto.analytics.AnalyticsEventMessage;
import com.lankaspot.server.enums.AnalyticsEventType;
import com.lankaspot.server.model.AnalyticsEvent;
import com.lankaspot.server.repository.AnalyticsEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@Slf4j
public class AnalyticsAggregationService {
    private final AnalyticsEventRepository eventRepository;
    private final EntityManager entityManager;
    private final Counter processed;
    private final Counter duplicates;
    private final Counter failures;

    public AnalyticsAggregationService(AnalyticsEventRepository eventRepository,
                                       EntityManager entityManager,
                                       MeterRegistry registry) {
        this.eventRepository = eventRepository;
        this.entityManager = entityManager;
        this.processed = registry.counter("lankaspot.analytics.events.processed");
        this.duplicates = registry.counter("lankaspot.analytics.events.duplicates");
        this.failures = registry.counter("lankaspot.analytics.events.failures");
    }

    @Transactional
    public void process(AnalyticsEventMessage message) {
        if (eventRepository.existsByEventId(message.eventId())) {
            duplicates.increment();
            log.info("Duplicate analytics event ignored eventId={} advertisementId={} eventType={}",
                    message.eventId(), message.advertisementId(), message.eventType());
            return;
        }

        AnalyticsEvent event = new AnalyticsEvent();
        event.setEventId(message.eventId());
        event.setAdvertisementId(message.advertisementId());
        event.setEventType(message.eventType());
        event.setVisitorKeyHash(message.visitorKeyHash());
        event.setSessionId(message.sessionId());
        event.setOccurredAt(message.occurredAt());
        event.setReceivedAt(Instant.now());
        event.setDurationSeconds(message.durationSeconds());

        try {
            eventRepository.saveAndFlush(event);
            LocalDate date = message.occurredAt().atZone(ZoneOffset.UTC).toLocalDate();

            if (message.eventType() == AnalyticsEventType.ADVERTISEMENT_VIEWED) {
                upsertDaily(message.advertisementId(), date,
                        "total_views", 1, "unique_views", 0,
                        "contact_retrievals", 0, "engaged_seconds", 0, "engaged_sessions", 0);

                int inserted = entityManager.createNativeQuery("""
                        INSERT INTO analytics_unique_views(advertisement_id, visitor_key_hash, view_date)
                        VALUES (:advertisementId, :visitorHash, :viewDate)
                        ON CONFLICT(advertisement_id, visitor_key_hash, view_date) DO NOTHING
                        """)
                        .setParameter("advertisementId", message.advertisementId())
                        .setParameter("visitorHash", message.visitorKeyHash())
                        .setParameter("viewDate", date)
                        .executeUpdate();
                if (inserted > 0) {
                    upsertDaily(message.advertisementId(), date,
                            "unique_views", 1, "total_views", 0,
                            "contact_retrievals", 0, "engaged_seconds", 0, "engaged_sessions", 0);
                }
            } else if (message.eventType() == AnalyticsEventType.CONTACT_RETRIEVED) {
                upsertDaily(message.advertisementId(), date,
                        "contact_retrievals", 1, "total_views", 0,
                        "unique_views", 0, "engaged_seconds", 0, "engaged_sessions", 0);
            } else if (message.eventType() == AnalyticsEventType.ADVERTISEMENT_ENGAGEMENT_COMPLETED) {
                int duration = Math.max(0, message.durationSeconds() == null ? 0 : message.durationSeconds());
                upsertDaily(message.advertisementId(), date,
                        "engaged_seconds", duration, "engaged_sessions", 1,
                        "total_views", 0, "unique_views", 0, "contact_retrievals", 0);
            }

            processed.increment();
            log.info("Analytics event processed eventId={} advertisementId={} eventType={} sessionId={}",
                    message.eventId(), message.advertisementId(), message.eventType(), message.sessionId());
        } catch (RuntimeException e) {
            failures.increment();
            log.error("Analytics event processing failed eventId={} advertisementId={} eventType={}",
                    message.eventId(), message.advertisementId(), message.eventType(), e);
            throw e;
        }
    }

    private void upsertDaily(Long advertisementId, LocalDate date,
                             String column1, long increment1,
                             String column2, long increment2,
                             String column3, long increment3,
                             String column4, long increment4,
                             String column5, long increment5) {
        String sql = """
                INSERT INTO advertisement_analytics_daily
                    (advertisement_id, analytics_date, total_views, unique_views, contact_retrievals,
                     engaged_seconds, engaged_sessions, created_at, updated_at)
                VALUES (:advertisementId, :date, 0, 0, 0, 0, 0, NOW(), NOW())
                ON CONFLICT (advertisement_id, analytics_date) DO UPDATE SET
                    total_views = advertisement_analytics_daily.total_views + :totalViews,
                    unique_views = advertisement_analytics_daily.unique_views + :uniqueViews,
                    contact_retrievals = advertisement_analytics_daily.contact_retrievals + :contactRetrievals,
                    engaged_seconds = advertisement_analytics_daily.engaged_seconds + :engagedSeconds,
                    engaged_sessions = advertisement_analytics_daily.engaged_sessions + :engagedSessions,
                    updated_at = NOW()
                """;
        entityManager.createNativeQuery(sql)
                .setParameter("advertisementId", advertisementId)
                .setParameter("date", date)
                .setParameter("totalViews", "total_views".equals(column1) ? increment1 : ("total_views".equals(column2) ? increment2 : ("total_views".equals(column3) ? increment3 : ("total_views".equals(column4) ? increment4 : ("total_views".equals(column5) ? increment5 : 0L)))))
                .setParameter("uniqueViews", "unique_views".equals(column1) ? increment1 : ("unique_views".equals(column2) ? increment2 : ("unique_views".equals(column3) ? increment3 : ("unique_views".equals(column4) ? increment4 : ("unique_views".equals(column5) ? increment5 : 0L)))))
                .setParameter("contactRetrievals", "contact_retrievals".equals(column1) ? increment1 : ("contact_retrievals".equals(column2) ? increment2 : ("contact_retrievals".equals(column3) ? increment3 : ("contact_retrievals".equals(column4) ? increment4 : ("contact_retrievals".equals(column5) ? increment5 : 0L)))))
                .setParameter("engagedSeconds", "engaged_seconds".equals(column1) ? increment1 : ("engaged_seconds".equals(column2) ? increment2 : ("engaged_seconds".equals(column3) ? increment3 : ("engaged_seconds".equals(column4) ? increment4 : ("engaged_seconds".equals(column5) ? increment5 : 0L)))))
                .setParameter("engagedSessions", "engaged_sessions".equals(column1) ? increment1 : ("engaged_sessions".equals(column2) ? increment2 : ("engaged_sessions".equals(column3) ? increment3 : ("engaged_sessions".equals(column4) ? increment4 : ("engaged_sessions".equals(column5) ? increment5 : 0L)))))
                .executeUpdate();
    }
}

