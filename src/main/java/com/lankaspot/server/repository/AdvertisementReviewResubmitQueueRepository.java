package com.lankaspot.server.repository;

import com.lankaspot.server.model.AdvertisementReviewResubmitQueue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvertisementReviewResubmitQueueRepository extends JpaRepository<AdvertisementReviewResubmitQueue, Long> {
    AdvertisementReviewResubmitQueue findByAdvertisementId(Long advertisementId);
    Page<AdvertisementReviewResubmitQueue> findAllByOrderByCreatedAtAscIdAsc(Pageable pageable);
}
