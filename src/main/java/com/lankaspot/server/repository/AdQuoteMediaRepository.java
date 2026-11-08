package com.lankaspot.server.repository;

import com.lankaspot.server.enums.MediaStatus;
import com.lankaspot.server.enums.ReviewerApprovalStatus;
import com.lankaspot.server.model.AdQuoteMedia;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface AdQuoteMediaRepository extends JpaRepository<AdQuoteMedia, Long> {
    // AdQuoteMediaRepository
    List<AdQuoteMedia> findByAdQuoteIdOrderBySortOrderAsc(Long adQuoteId);
    List<AdQuoteMedia> findByAdQuote_IdOrderBySortOrderAscIdAsc(Long quoteId);
    List<AdQuoteMedia> findByAdQuote_IdAndMediaStatusNotOrderBySortOrderAscIdAsc(Long quoteId, MediaStatus mediaStatus);
    List<AdQuoteMedia> findByAdQuote_IdAndReviewerApprovalStatusNotOrderBySortOrderAscIdAsc(
            Long quoteId,
            ReviewerApprovalStatus status
    );

    long countByAdQuote_Id(Long quoteId);

    Optional<AdQuoteMedia> findByIdAndAdQuote_Id(Long mediaId, Long quoteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m from AdQuoteMedia m
            where m.adQuote.id = :quoteId
            order by m.sortOrder asc, m.id asc
            """)
    List<AdQuoteMedia> findAllByQuoteIdForUpdate(
            @Param("quoteId") Long quoteId
    );

    @Query("""
            select m from AdQuoteMedia m
            where m.adQuote.id = :quoteId
            order by m.sortOrder asc, m.id asc
            """)
    List<AdQuoteMedia> findAllByQuoteId(
            @Param("quoteId") Long quoteId
    );

    @Modifying
    @Transactional
    @Query("""
    UPDATE AdQuoteMedia m
    SET m.mediaStatus = :status
    WHERE m.id = :mediaId
    """)
    int updateMediaApprovalStatus(
            @Param("mediaId") Long mediaId,
            @Param("status") MediaStatus status
    );


    @Modifying
    @Transactional
    @Query("""
    UPDATE AdQuoteMedia m
    SET m.reviewerApprovalStatus = :status
    WHERE m.id = :mediaId
    """)
    int updateReviewerApprovalStatus(
            @Param("mediaId") Long mediaId,
            @Param("status") ReviewerApprovalStatus status
    );



}
