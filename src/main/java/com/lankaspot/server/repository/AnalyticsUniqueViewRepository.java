package com.lankaspot.server.repository;

import com.lankaspot.server.model.AnalyticsUniqueView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface AnalyticsUniqueViewRepository extends JpaRepository<AnalyticsUniqueView, Long> {
    boolean existsByAdvertisementIdAndVisitorKeyHashAndViewDate(Long advertisementId, String visitorKeyHash, LocalDate viewDate);
}

