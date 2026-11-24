package com.lankaspot.server.service;

import com.lankaspot.server.dto.analytics.AdvertisementAnalyticsResponse;
import com.lankaspot.server.exception.AdvertisementNotFoundException;
import com.lankaspot.server.exception.BadRequestException;
import com.lankaspot.server.model.Advertisement;
import com.lankaspot.server.model.AdvertisementAnalyticsDaily;
import com.lankaspot.server.repository.AdvertisementAnalyticsDailyRepository;
import com.lankaspot.server.repository.AdvertisementRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AdvertisementAnalyticsReportingService {
    private final AdvertisementRepository advertisementRepository;
    private final AdvertisementAnalyticsDailyRepository dailyRepository;

    public AdvertisementAnalyticsReportingService(AdvertisementRepository advertisementRepository,
                                                   AdvertisementAnalyticsDailyRepository dailyRepository) {
        this.advertisementRepository = advertisementRepository;
        this.dailyRepository = dailyRepository;
    }

    public AdvertisementAnalyticsResponse report(UUID advertisementRef, LocalDate from, LocalDate to,
                                                 Authentication authentication) {
        if (from.isAfter(to)) throw new BadRequestException("INVALID_ANALYTICS_DATE_RANGE", "from date must not be after to date");

        Advertisement ad = advertisementRepository.findByAdvertisementRef(advertisementRef)
                .orElseThrow(() -> new AdvertisementNotFoundException(advertisementRef.toString()));

        if (ad.getAdvertiser() == null || !ad.getAdvertiser().getKeycloakUserId().equals(authentication.getName())) {
            throw new AdvertisementNotFoundException(advertisementRef.toString());
        }

        List<AdvertisementAnalyticsDaily> rows = dailyRepository
                .findByAdvertisementIdAndAnalyticsDateBetweenOrderByAnalyticsDateAsc(ad.getId(), from, to);

        long totalViews = rows.stream().mapToLong(AdvertisementAnalyticsDaily::getTotalViews).sum();
        long uniqueViews = rows.stream().mapToLong(AdvertisementAnalyticsDaily::getUniqueViews).sum();
        long contacts = rows.stream().mapToLong(AdvertisementAnalyticsDaily::getContactRetrievals).sum();
        long engagedSeconds = rows.stream().mapToLong(AdvertisementAnalyticsDaily::getEngagedSeconds).sum();
        long engagedSessions = rows.stream().mapToLong(AdvertisementAnalyticsDaily::getEngagedSessions).sum();
        double average = engagedSessions == 0 ? 0.0 : (double) engagedSeconds / engagedSessions;

        List<AdvertisementAnalyticsResponse.DailyPerformance> daily = rows.stream()
                .map(row -> new AdvertisementAnalyticsResponse.DailyPerformance(
                        row.getAnalyticsDate(), row.getTotalViews(), row.getUniqueViews(),
                        row.getContactRetrievals(), row.getEngagedSeconds(), row.getEngagedSessions()))
                .toList();

        return new AdvertisementAnalyticsResponse(totalViews, uniqueViews, contacts,
                engagedSeconds, average, daily);
    }
}
