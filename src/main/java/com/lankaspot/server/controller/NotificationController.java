package com.lankaspot.server.controller;

import com.lankaspot.server.dto.notification.GetAllNotificationsDTO;
import com.lankaspot.server.response.LankaspotResponse;
import com.lankaspot.server.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("hasAnyRole('ADVERTISER', 'REVIEWER')")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/getAll")
    public ResponseEntity<LankaspotResponse<GetAllNotificationsDTO>> getAllNotifications(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getAllNotifications(authentication));
    }

}
