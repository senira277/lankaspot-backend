package com.lankaspot.server.controller;

import com.lankaspot.server.dto.notification.CommonPageResponse;
import com.lankaspot.server.dto.notification.GetAllNotificationsDTO;
import com.lankaspot.server.response.LankaspotResponse;
import com.lankaspot.server.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("hasAnyRole('ADVERTISER', 'REVIEWER')")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/getAll")
    public ResponseEntity<LankaspotResponse<GetAllNotificationsDTO>> getAllNotifications(@RequestParam int page, @RequestParam int size, Authentication authentication) {
        return ResponseEntity.ok(notificationService.getAllNotifications(authentication,page,size));
    }

    @PatchMapping("/markPageSeen")
    public ResponseEntity<LankaspotResponse<CommonPageResponse>> markPageSeen(@RequestParam int page, @RequestParam int size, Authentication authentication) {
        return ResponseEntity.ok(notificationService.markPageSeen(authentication,page,size));
    }

    @PatchMapping("/markSingleSeen")
    public ResponseEntity<LankaspotResponse<Boolean>> markSingleSeen(@RequestParam String id, Authentication authentication) {
        return ResponseEntity.ok(notificationService.markSingleSeen(authentication,id));
    }

}
