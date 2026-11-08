package com.lankaspot.server.repository;

import com.lankaspot.server.model.AdvertisementReviewQueue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvertisementReviewQueueRepository extends JpaRepository<AdvertisementReviewQueue, Long> {
    AdvertisementReviewQueue findByAdvertisementId(Long advertisementId);
    Page<AdvertisementReviewQueue> findAllByOrderByCreatedAtAscIdAsc(
            Pageable pageable
    );

}
