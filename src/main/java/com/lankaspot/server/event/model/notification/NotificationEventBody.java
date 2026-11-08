package com.lankaspot.server.event.model.notification;

import com.lankaspot.server.model.Notification;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public record NotificationEventBody(
        Notification notification,
        UUID userId
) {
}
