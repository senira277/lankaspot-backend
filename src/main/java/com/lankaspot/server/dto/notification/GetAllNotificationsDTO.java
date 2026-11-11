package com.lankaspot.server.dto.notification;

import com.lankaspot.server.model.Notification;
import org.springframework.data.domain.Page;

import java.util.List;

public record GetAllNotificationsDTO(
        Page<Notification> notifications,
        int unreadCount,
        Long page,
        Long size,
        Long totalPages,
        Long totalElements
) {
}
