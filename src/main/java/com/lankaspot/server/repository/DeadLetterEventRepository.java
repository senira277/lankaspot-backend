package com.lankaspot.server.repository;

import com.lankaspot.server.model.DeadLetterEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, UUID> {
    boolean existsByOriginalTopicAndConsumerGroupAndPartitionNumberAndOffsetNumber(
            String originalTopic,
            String consumerGroup,
            Integer partitionNumber,
            Long offsetNumber
    );
}
