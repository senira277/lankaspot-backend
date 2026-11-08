package com.lankaspot.server.repository;

import com.lankaspot.server.model.ProcessedEvent;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    Optional<ProcessedEvent> findByEventId(UUID eventId);

    @Modifying
    @Query(value = """
            INSERT INTO lankaspot_db.processed_events
                (event_id, accepted_at, status)
            VALUES
                (:eventId, CURRENT_TIMESTAMP, 'ACCEPTED')
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("eventId") UUID eventId);
}
