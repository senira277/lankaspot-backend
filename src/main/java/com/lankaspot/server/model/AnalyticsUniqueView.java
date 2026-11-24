package com.lankaspot.server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "analytics_unique_views", uniqueConstraints = @UniqueConstraint(
        name = "uk_analytics_unique_view",
        columnNames = {"advertisement_id", "visitor_key_hash", "view_date"}))
@Getter
@Setter
@NoArgsConstructor
public class AnalyticsUniqueView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "advertisement_id", nullable = false)
    private Long advertisementId;

    @Column(name = "visitor_key_hash", nullable = false, length = 64)
    private String visitorKeyHash;

    @Column(name = "view_date", nullable = false)
    private LocalDate viewDate;
}

