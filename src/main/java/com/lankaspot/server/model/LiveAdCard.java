package com.lankaspot.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Denormalized public listing projection. The advertisement UUID is kept as a
 * scalar so listing reads never need to hydrate the Advertisement aggregate.
 */
@Entity
@Table(name = "live_ad_cards", indexes = {
        @Index(name = "idx_live_ad_cards_category", columnList = "category_id"),
        @Index(name = "idx_live_ad_cards_city", columnList = "city_id"),
        @Index(name = "idx_live_ad_cards_district", columnList = "district_id"),
        @Index(name = "idx_live_ad_cards_listing_order", columnList = "sort_order, queue_number, advertisement_id"),
        @Index(name = "idx_live_ad_cards_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LiveAdCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "advertisement_id", nullable = false, unique = true, updatable = false)
    private UUID advertisementId;
    @Column(name = "functional_group_id")
    private Long functionalGroupId;
    @Column(name = "functional_group_name", length = 100)
    private String functionalGroupName;
    @Column(name = "category_id", nullable = false)
    private Long categoryId;
    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;
    @Column(name = "city_id", nullable = false)
    private Long cityId;
    @Column(name = "city_name", nullable = false, length = 100)
    private String cityName;
    @Column(name = "district_id", nullable = false)
    private Long districtId;
    @Column(name = "district_name", nullable = false, length = 100)
    private String districtName;
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    @Column(name = "nearest_landmark", length = 100)
    private String nearestLandmark;
    @Column(name = "town", length = 100)
    private String town;
    @Column(name = "price", precision = 15, scale = 2)
    private BigDecimal price;
    @Column(name = "slug", nullable = false, length = 250)
    private String slug;
    @Column(name = "quote_ref", nullable = false)
    private String quoteRef;
    @Column(name = "cover_url", columnDefinition = "TEXT")
    private String coverUrl;
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
    @Column(name = "queue_number", nullable = false)
    private Long queueNumber = 0L;
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;


}
