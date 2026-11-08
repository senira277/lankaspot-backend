package com.lankaspot.server.dto.notification;

import com.lankaspot.server.model.Notification;

import java.util.List;

public record GetAllNotificationsDTO(
        List<Notification> notifications,
        int unreadCount
) {
}
