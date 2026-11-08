package com.lankaspot.server.repository;

import com.lankaspot.server.enums.AdvertisementStatus;
import com.lankaspot.server.model.Advertisement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    Optional<Advertisement> findByAdvertisementRef(UUID advertisementRef);

    Page<Advertisement> findByAdvertiser_Id(Long advertiserId, Pageable pageable);

    boolean existsByAdvertisementRefAndAdvertiser_Id(
            UUID advertisementRef,
            Long advertiserId
    );

    @Modifying
    @Transactional
    @Query("""
        UPDATE Advertisement a
        SET a.status = :status
        WHERE a.id = :id
        """)
    int updateStatusById(
            @Param("id") Long id,
            @Param("status") AdvertisementStatus status
    );
}