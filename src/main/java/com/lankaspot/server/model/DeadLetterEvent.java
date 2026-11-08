package com.lankaspot.server.model;


import com.lankaspot.server.enums.DeadLetterStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "dead_letter_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "original_topic",
                                "consumer_group",
                                "partition_number",
                                "offset_number"
                        }
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID eventId;

    @Column(nullable = false)
    private String originalTopic;

    private String consumerGroup;

    private Integer partitionNumber;

    private Long offsetNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;

    private String exceptionClass;

    @Column(columnDefinition = "TEXT")
    private String exceptionMessage;

    @Column(nullable = false)
    private Instant failedAt;

    @Enumerated(EnumType.STRING)
    private DeadLetterStatus status;
}