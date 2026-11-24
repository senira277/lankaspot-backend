package com.lankaspot.server.repository;

import com.lankaspot.server.enums.LiveTopAdStatus;
import com.lankaspot.server.model.Category;
import com.lankaspot.server.model.LiveTopAd;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LiveTopAdRepository extends JpaRepository<LiveTopAd, Long> {
    long countByCategoryAndStatusNot(Category category, LiveTopAdStatus status);

    List<LiveTopAd> findByStatusAndExpireAtAfterOrderByCreatedAtAsc(
            LiveTopAdStatus status,
            LocalDateTime now
    );

    List<LiveTopAd> findByStatusAndExpireAtLessThanEqual(
            LiveTopAdStatus status,
            LocalDateTime now
    );

    long countByCategoryAndStatusAndExpireAtAfter(
            Category category,
            LiveTopAdStatus status,
            LocalDateTime now
    );

    Optional<LiveTopAd> findByAdvertisement_AdvertisementRefAndStatus(
            UUID advertisementRef,
            LiveTopAdStatus status
    );
}
