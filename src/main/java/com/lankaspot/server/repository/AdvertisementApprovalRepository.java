package com.lankaspot.server.repository;

import com.lankaspot.server.model.AdvertisementApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdvertisementApprovalRepository extends JpaRepository<AdvertisementApproval, Long> {
    Optional<AdvertisementApproval> findFirstByPolicyIdOrderByDecisionAtDesc(Long policyId);
    //find the latest approval for a given advertisement
    Optional<AdvertisementApproval> findFirstByAdvertisement_IdOrderByDecisionAtDesc(Long advertisementId);}
