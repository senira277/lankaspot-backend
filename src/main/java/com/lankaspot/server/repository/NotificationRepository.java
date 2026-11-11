package com.lankaspot.server.repository;

import com.lankaspot.server.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {


    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);
    List<Notification> findByRecipientIdAndSeenOrderByCreatedAtDesc(UUID recipientId,boolean seen);
    void  deleteByRecipientIdAndId(UUID recipientId, Long id);




    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
    UPDATE Notification n
    SET n.seen = true,
        n.seenAt = CURRENT_TIMESTAMP
    WHERE n.recipientId = :recipientId
      AND n.seen = false
""")
    int markAllAsSeen(@Param("recipientId") UUID recipientId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
    DELETE FROM Notification n
    WHERE n.recipientId = :recipientId
      AND n.seen = true
""")
    int deleteAllSeen(@Param("recipientId") UUID recipientId);

    int countByRecipientIdAndSeenFalse(UUID recipientId);

    Optional<Notification> findByRecipientIdAndId(UUID recipientId, Long id);

}
