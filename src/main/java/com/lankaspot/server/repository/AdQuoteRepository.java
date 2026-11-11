package com.lankaspot.server.repository;
import com.lankaspot.server.enums.AdQuoteStatus;
import com.lankaspot.server.enums.PaymentRegistryStatus;
import com.lankaspot.server.enums.PaymentStatus;
import com.lankaspot.server.enums.ReviewerApprovalStatus;
import com.lankaspot.server.model.AdQuote;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface AdQuoteRepository extends JpaRepository<AdQuote, Long> {
    List<AdQuote> findByAdvertisementRefOrderByVersionAsc(UUID advertisementRef);
    @Query("select q from AdQuote q where not exists " +
            "(select m.id from EntityMapping m where m.quote = q)")
    List<AdQuote> findUnconverted();
    Optional<AdQuote> findTopByAdvertisementRefOrderByVersionDesc(UUID advertisementRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from AdQuote q where q.id = :quoteRef")
    Optional<AdQuote> findByIdForUpdate(@Param("quoteRef") Long quoteRef);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update AdQuote q
    set q.lastCompletedStep = :step,
        q.updatedAt = CURRENT_TIMESTAMP
    where q.id = :quoteId
      and q.lastCompletedStep < :step
    """)
    int updateLastCompletedStep(@Param("quoteId") Long quoteId, @Param("step") Integer step);

    Optional<AdQuote> findFirstByAdvertiserIdAndStatusAndLastCompletedStepGreaterThanOrderByUpdatedAtDesc(
            UUID advertiserId, AdQuoteStatus status, Integer lastCompletedStep);

    @Query("""
    select q from AdQuote q
    where q.advertiserId = :advertiserId
      and q.status = :status
      and q.lastCompletedStep > :minStep
      and not exists (
          select 1 from EntityMapping m
          where m.quote = q
      )
      and not exists (
          select 1 from PaymentRegistry p
          where p.quoteRef = q.id
            and p.status = :successfulPaymentStatus
      )
    order by q.updatedAt desc
    """)
    List<AdQuote> findResumeCandidates(
            @Param("advertiserId") UUID advertiserId,
            @Param("status") AdQuoteStatus status,
            @Param("minStep") Integer minStep,
            @Param("successfulPaymentStatus") PaymentRegistryStatus successfulPaymentStatus,
            Pageable pageable
    );

    default Optional<AdQuote> findLatestResumableDraft(UUID advertiserId) {
        return findResumeCandidates(
                advertiserId,
                AdQuoteStatus.IN_COMPLETE,
                0,
                PaymentRegistryStatus.SUCCESS,
                PageRequest.of(0, 1)
        ).stream().findFirst();
    }

    @Modifying
    @Transactional
    @Query("""
    UPDATE AdQuote q
    SET q.reviewerApprovalStatus = :approvalStatus,
        q.status = :status
    WHERE q.id = :quoteId
    """)
    int updateQuoteApprovalStatusAndStatus(
            @Param("quoteId") Long quoteId,
            @Param("approvalStatus") ReviewerApprovalStatus approvalStatus,
            @Param("status") AdQuoteStatus status
    );
    boolean existsByIdAndAdvertiserId(Long quoteId, UUID advertiserId);





}
