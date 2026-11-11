package com.lankaspot.server.service.notification;

import com.lankaspot.server.dto.notification.CommonPageResponse;
import com.lankaspot.server.dto.notification.GetAllNotificationsDTO;
import com.lankaspot.server.enums.NotificationType;
import com.lankaspot.server.enums.Role;
import com.lankaspot.server.exception.QuoteNotFoundException;
import com.lankaspot.server.model.AdQuote;
import com.lankaspot.server.model.AdQuoteMedia;
import com.lankaspot.server.model.Notification;
import com.lankaspot.server.repository.AdQuoteMediaRepository;
import com.lankaspot.server.repository.AdQuoteRepository;
import com.lankaspot.server.repository.NotificationRepository;
import com.lankaspot.server.response.LankaspotResponse;
import com.lankaspot.server.service.AdvertisementService;
import com.lankaspot.server.service.UtilService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
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
    public Page<Notification> findByUser(Authentication authentication, int page, int size) {
        UUID userId = UUID.fromString(authentication.getName());
        //list of Notifications
        return  notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, PageRequest.of(page,size));

    }

    public Page<Notification> findByRecipientId(UUID recipientId, int page, int size) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, PageRequest.of(page,size));
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
    public LankaspotResponse<GetAllNotificationsDTO> getAllNotifications(Authentication authentication, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }

        if (size < 1) {
            throw new IllegalArgumentException("Size must be greater than zero");
        }
        Page<Notification> notifications = findByRecipientId(utilService.resolveUUID(authentication), page, size);
        int unreadCount = unreadCount(authentication);

        return LankaspotResponse.success(
                new GetAllNotificationsDTO(notifications, unreadCount, (long) page, (long) size, (long) notifications.getTotalPages(), notifications.getTotalElements()),
                "Successfully Retrieved all notifications"
        );
    }

    @Transactional
    public LankaspotResponse<CommonPageResponse> markPageSeen(Authentication authentication, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }

        if (size < 1) {
            throw new IllegalArgumentException("Size must be greater than zero");
        }

        Page<Notification> notifications = findByRecipientId(utilService.resolveUUID(authentication), page, size);
        notifications.forEach(this::markNotificationAsSeen);

        return LankaspotResponse.success(new CommonPageResponse(page, size), "Successfully marked notifications as seen for page: " + page);
    }

    @Transactional
    public LankaspotResponse<Boolean> markSingleSeen(Authentication authentication, String notificationId) {
        UUID userId = utilService.resolveUUID(authentication);
        Notification notification = notificationRepository.findByRecipientIdAndId(userId, Long.parseLong(notificationId))
                .orElseThrow(() -> new IllegalArgumentException("Notification not found for the given ID and user"));

        markNotificationAsSeen(notification);
        return LankaspotResponse.success(true, "Successfully marked notification as seen for ID: " + notificationId);
    }


}
