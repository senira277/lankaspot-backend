package com.lankaspot.server.dto.analytics;

import java.time.LocalDate;
import java.util.List;

public record AdvertisementAnalyticsResponse(
        long totalViews,
        long uniqueViews,
        long contactRetrievals,
        long engagedSeconds,
        double averageEngagementDuration,
        List<DailyPerformance> daily
) {
    public record DailyPerformance(
            LocalDate date,
            long totalViews,
            long uniqueViews,
            long contactRetrievals,
            long engagedSeconds,
            long engagedSessions
    ) {}
}

