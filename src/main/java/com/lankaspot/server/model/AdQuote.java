package com.lankaspot.server.model;

import com.lankaspot.server.enums.AdQuoteStatus;
import com.lankaspot.server.enums.AdSource;
import com.lankaspot.server.enums.ReviewerApprovalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable content snapshot. Commercial terms belong to AdPolicy.
 */
@Entity
@Table(name = "ad_quotes",
        uniqueConstraints = @UniqueConstraint(name = "uq_ad_quote_version",
                columnNames = {"advertisement_ref", "version"}))
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class AdQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //map to functional group table
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "functional_group_id", nullable = true)
    private FunctionalGroup functionalGroup;

    @Column(name = "advertisement_ref", nullable = false, updatable = false)
    private UUID advertisementRef;

    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(name = "advertiser_id", updatable = false)
    private UUID advertiserId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, updatable = false)
    private Category category;

    @Column(name = "title", nullable = false, length = 200, updatable = false)
    private String title;

    @Column(name = "slug", nullable = false, length = 250, updatable = false)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT", updatable = false)
    private String description;

    @Column(name = "price", precision = 15, scale = 2, updatable = false)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false, updatable = false)
    private City city;

    @Column(name = "town", nullable = false, length = 100, updatable = false)
    private String town;

    @Column(name = "nearest_landmark", nullable = false, length = 100, updatable = false)
    private String nearestLandmark;

    @Column(name = "address", columnDefinition = "TEXT", updatable = false)
    private String address;

    @OneToMany(mappedBy = "adQuote", cascade = CascadeType.PERSIST)
    private Set<AdQuoteFeature> features = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AdQuoteStatus status = AdQuoteStatus.IN_COMPLETE;

    @Column(name = "cache_version")
    private Long cacheVersion = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private AdSource source = AdSource.USER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_completed_step", nullable = false)
    private Integer lastCompletedStep = 0;

    @Column(name = "last_completed_amendment_step")
    private Integer lastCompletedAmendmentStep = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "reviewer_approval_status", length = 20)
    private ReviewerApprovalStatus reviewerApprovalStatus = ReviewerApprovalStatus.PENDING_REVIEW;


    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }
    @PreUpdate
    void initializeUpdatedAt() {
        updatedAt = LocalDateTime.now();
    }

    /** Marks a wizard step done. Never moves backwards when an earlier step is re-saved. */
    public void completeStep(int step) {
        if (step > lastCompletedStep) lastCompletedStep = step;
        touch();
    }

    public void completeAmendmentStep(int step) {
        if (step > lastCompletedAmendmentStep) lastCompletedAmendmentStep = step;
        touch();
    }

    /** Forces a dirty update so updated_at moves even when nothing else changed. */
    public void touch() {
        updatedAt = LocalDateTime.now();
    }

    public int nextAvailableVersion() {
        return version + 1;
    }
}
