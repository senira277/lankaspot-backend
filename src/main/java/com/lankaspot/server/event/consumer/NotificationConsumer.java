package com.lankaspot.server.event.consumer;


import com.lankaspot.server.enums.NotificationType;
import com.lankaspot.server.event.model.notification.NotificationEvent;
import com.lankaspot.server.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {
    private final NotificationService notificationService;

    public void consume(NotificationEvent event) {

        NotificationType type = event.body().notification().getType();

        switch (type) {
            case CREATE_ADVERTISEMENT_PURCHASED_SUCCESSFULLY, ADVERTISEMENT_APPROVED, ADVERTISEMENT_REJECTED -> notificationService.createNotification(event.body().notification());

            default -> throw new IllegalArgumentException("Invalid notification type");
        }

    }



}
