
package com.lankaspot.server.model;

import com.lankaspot.server.enums.ProcessedEventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "processed_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_processed_events_event_id",
                        columnNames = "event_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_processed_events_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_processed_events_accepted_at",
                        columnList = "accepted_at"
                ),
                @Index(
                        name = "idx_processed_events_status_accepted_at",
                        columnList = "status, accepted_at"
                )
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "event_id",
            nullable = false,
            updatable = false
    )
    private UUID eventId;

    @Column(
            name = "accepted_at",
            nullable = false,
            updatable = false
    )
    private Instant acceptedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 50
    )
    private ProcessedEventStatus status;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }

        if (acceptedAt == null) {
            acceptedAt = Instant.now();
        }

        if (status == null) {
            status = ProcessedEventStatus.ACCEPTED;
        }
    }
}
