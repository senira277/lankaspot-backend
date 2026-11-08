package com.lankaspot.server.model;

import com.lankaspot.server.enums.NotificationType;
import com.lankaspot.server.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(
                        name = "idx_notification_recipient_created",
                        columnList = "recipient_id, created_at"
                ),
                @Index(
                        name = "idx_notification_recipient_seen",
                        columnList = "recipient_id, seen"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The actual user receiving this notification.
     */
    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    /**
     * The role/context in which this notification is displayed.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_role", nullable = false, length = 30)
    private Role recipientRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Internal frontend route, e.g. /advertiser/advertisements/123
     */
    @Column(name = "redirect_to", length = 500)
    private String redirectTo;

    @Builder.Default
    @Column(nullable = false)
    private boolean seen = false;

    @Column(name = "seen_at")
    private Instant seenAt;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    /**
     * Additional type-specific data.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void markAsSeen() {
        if (!seen) {
            seen = true;
            seenAt = Instant.now();
        }
    }
}