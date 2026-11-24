package com.lankaspot.server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "advertisement_analytics_daily", uniqueConstraints = @UniqueConstraint(
        name = "uk_advertisement_analytics_daily",
        columnNames = {"advertisement_id", "analytics_date"}))
@Getter
@Setter
@NoArgsConstructor
public class AdvertisementAnalyticsDaily {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "advertisement_id", nullable = false)
    private Long advertisementId;

    @Column(name = "analytics_date", nullable = false)
    private LocalDate analyticsDate;

    @Column(name = "total_views", nullable = false)
    private long totalViews;

    @Column(name = "unique_views", nullable = false)
    private long uniqueViews;

    @Column(name = "contact_retrievals", nullable = false)
    private long contactRetrievals;

    @Column(name = "engaged_seconds", nullable = false)
    private long engagedSeconds;

    @Column(name = "engaged_sessions", nullable = false)
    private long engagedSessions;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

