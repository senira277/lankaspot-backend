package com.lankaspot.server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ad_quote_tiers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"quote_id", "advertisement_tier_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class AdQuoteTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    private AdQuote adQuote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "advertisement_tier_id", nullable = false)
    private AdvertisementTier advertisementTier;

    //createdAt to determine the expiry
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}