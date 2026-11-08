package com.lankaspot.server.service.notification;

import com.lankaspot.server.constants.NotificationConstants;
import com.lankaspot.server.dto.notification.GetAllNotificationsDTO;
import com.lankaspot.server.enums.NotificationType;
import com.lankaspot.server.enums.Role;
import com.lankaspot.server.exception.AdvertisementNotFoundException;
import com.lankaspot.server.exception.QuoteNotFoundException;
import com.lankaspot.server.model.AdQuote;
import com.lankaspot.server.model.AdQuoteMedia;
import com.lankaspot.server.model.Notification;
import com.lankaspot.server.model.PaymentRegistry;
import com.lankaspot.server.repository.AdQuoteMediaRepository;
import com.lankaspot.server.repository.AdQuoteRepository;
import com.lankaspot.server.repository.NotificationRepository;
import com.lankaspot.server.response.LankaspotResponse;
import com.lankaspot.server.service.AdvertisementService;
import com.lankaspot.server.service.UtilService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UtilService utilService;
    private final AdvertisementService advertisementService;
    private final AdQuoteRepository adQuoteRepository;
    private final AdQuoteMediaRepository adQuoteMediaRepository;


    public Notification findById(Long id) {
        return notificationRepository.findById(id).orElse(null);
    }

    // get all notifications for computed role
    public List<Notification> findByUser(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());

        //list of Notifications
        return  notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);


    }

    public List<Notification> findByRecipientId(UUID recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }

    public List<Notification> getAllSeenNotifications(Authentication authentication) {
        return notificationRepository.findByRecipientIdAndSeenOrderByCreatedAtDesc(utilService.resolveUUID(authentication),true);
    }

    public List<Notification> getAllUnseenNotifications(Authentication authentication) {
        return notificationRepository.findByRecipientIdAndSeenOrderByCreatedAtDesc(utilService.resolveUUID(authentication),false);
    }

    public void markNotificationAsSeen(Notification notification) {
        notification.setSeen(true);
        notificationRepository.save(notification);
    }

    public void markAllUnseenNotificationsAsSeen(Authentication authentication) {
        int updateCount = notificationRepository.markAllAsSeen(utilService.resolveUUID(authentication));
        if (updateCount > 0) {
            log.info("markAllUnseenNotificationsAsSeen : UPDATE COUNT " + updateCount);
        }
    }

    public void deleteNotification(Authentication authentication, Notification notification) {
        notificationRepository.deleteByRecipientIdAndId(utilService.resolveUUID(authentication), notification.getId());
    }

    public void deleteAllSeenNotifications(Authentication authentication) {
        notificationRepository.deleteAllSeen(utilService.resolveUUID(authentication));
    }

    public void createNotification(Notification notification) {
        notificationRepository.save(notification);
    }

    public int unreadCount(Authentication authentication) {
        return notificationRepository.countByRecipientIdAndSeenFalse(utilService.resolveUUID(authentication));
    }

    public Notification createNotificationForAdvertiser(String advertisementRef, UUID userId, String title, String Description,String redirectTo, NotificationType notificationType) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setDescription(Description);
        notification.setRecipientId(userId);
        notification.setRecipientRole(Role.ADVERTISER);
        notification.setRedirectTo(redirectTo);
        notification.setType(notificationType);



        AdQuote quote = adQuoteRepository.findById(advertisementService.findQuoteIdByAdvertisementRef(advertisementRef))
                .orElseThrow(() -> {
                    log.warn("Quote not found. advertisementRef={}", advertisementRef);
                    return new QuoteNotFoundException(advertisementService.findQuoteIdByAdvertisementRef(advertisementRef));
                });

        String coverUrl = adQuoteMediaRepository
                .findAllByQuoteId(quote.getId())
                .stream()
                .filter(AdQuoteMedia::isPrimary)
                .map(AdQuoteMedia::getUrl)
                .findFirst()
                .orElse(null);

        notification.setImageUrl(coverUrl);

        return notification;

    }


    //SERVICE METHODS
    @Transactional(readOnly = true)
    public LankaspotResponse<GetAllNotificationsDTO> getAllNotifications(Authentication authentication) {
        List<Notification> notifications = findByRecipientId(utilService.resolveUUID(authentication));
        int unreadCount = unreadCount(authentication);

        return LankaspotResponse.success(
                new GetAllNotificationsDTO(notifications, unreadCount),
                "Successfully Retrieved all notifications"
        );
    }


}
