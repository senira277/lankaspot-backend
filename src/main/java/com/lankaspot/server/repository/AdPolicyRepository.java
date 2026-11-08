package com.lankaspot.server.repository;
import com.lankaspot.server.model.AdPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdPolicyRepository extends JpaRepository<AdPolicy, Long> {
    java.util.List<AdPolicy> findByAdvertisementAdvertiserIdOrderByCreatedAtDesc(Long advertiserId);
    Optional<AdPolicy> findByAdvertisement_Id(Long advertisementId);
}
