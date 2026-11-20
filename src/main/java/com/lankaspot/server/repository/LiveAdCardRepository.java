package com.lankaspot.server.repository;

import com.lankaspot.server.model.LiveAdCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LiveAdCardRepository extends JpaRepository<LiveAdCard, Long> {
    Optional<LiveAdCard> findByAdvertisementId(UUID advertisementId);
    void deleteByAdvertisementId(UUID advertisementId);

    @Query("""
        select c from LiveAdCard c join Advertisement a
          on a.advertisementRef = c.advertisementId
        where a.status = com.lankaspot.server.enums.AdvertisementStatus.ACTIVE
          and (c.expiresAt is null or c.expiresAt > :now)
          and (:categoryId is null or c.categoryId = :categoryId)
          and (:cityId is null or c.cityId = :cityId)
          and (:districtId is null or c.districtId = :districtId)
          and (:minPrice is null or c.price >= :minPrice)
          and (:maxPrice is null or c.price <= :maxPrice)
        order by c.sortOrder asc, c.queueNumber asc, c.advertisementId asc
        """)
    Page<LiveAdCard> search(@Param("categoryId") Long categoryId,
                            @Param("cityId") Long cityId,
                            @Param("districtId") Long districtId,
                            @Param("minPrice") java.math.BigDecimal minPrice,
                            @Param("maxPrice") java.math.BigDecimal maxPrice,
                            @Param("now") LocalDateTime now,
                            Pageable pageable);

    @Query("""
        select new com.lankaspot.server.dto.listings.ListingFeatureDTO(
          qf.adQuote.advertisementRef, f.name, qf.value, f.unit)
        from AdQuoteFeature qf join qf.feature f
        where qf.adQuote.advertisementRef in :advertisementIds
        order by qf.adQuote.advertisementRef, f.sortOrder, f.id
        """)
    List<com.lankaspot.server.dto.listings.ListingFeatureDTO> findFeaturesByAdvertisementIds(
            @Param("advertisementIds") Collection<UUID> advertisementIds);

    @Query("""
        select new com.lankaspot.server.dto.listings.ListingTierDTO(
          qt.adQuote.advertisementRef, qt.advertisementTier.id,
          qt.advertisementTier.code, qt.createdAt, qt.advertisementTier.durationDays)
        from AdQuoteTier qt
        where qt.adQuote.advertisementRef in :advertisementIds
          and qt.advertisementTier.active = true
        order by qt.adQuote.advertisementRef, qt.advertisementTier.id
        """)
    List<com.lankaspot.server.dto.listings.ListingTierDTO> findTiersByAdvertisementIds(
            @Param("advertisementIds") Collection<UUID> advertisementIds);
}
