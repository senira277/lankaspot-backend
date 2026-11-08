package com.lankaspot.server.event.model.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationEvent(
        UUID id,
        Instant publishedAt,
        NotificationEventBody body
) {
}
