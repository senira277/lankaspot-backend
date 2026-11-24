package com.lankaspot.server.repository;

import com.lankaspot.server.model.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {
    boolean existsByEventId(UUID eventId);
}

