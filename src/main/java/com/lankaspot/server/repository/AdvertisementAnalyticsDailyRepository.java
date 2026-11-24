package com.lankaspot.server.repository;

import com.lankaspot.server.model.AdvertisementAnalyticsDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AdvertisementAnalyticsDailyRepository extends JpaRepository<AdvertisementAnalyticsDaily, Long> {
    Optional<AdvertisementAnalyticsDaily> findByAdvertisementIdAndAnalyticsDate(Long advertisementId, LocalDate analyticsDate);
    List<AdvertisementAnalyticsDaily> findByAdvertisementIdAndAnalyticsDateBetweenOrderByAnalyticsDateAsc(Long advertisementId, LocalDate from, LocalDate to);
}

