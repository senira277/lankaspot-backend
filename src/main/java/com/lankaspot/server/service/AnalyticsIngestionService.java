package com.lankaspot.server.service;

import com.lankaspot.server.dto.analytics.AnalyticsEventMessage;
import com.lankaspot.server.dto.analytics.EngagementStartResponse;
import com.lankaspot.server.enums.AnalyticsEventType;
import com.lankaspot.server.exception.AdvertisementNotFoundException;
import com.lankaspot.server.exception.BadRequestException;
import com.lankaspot.server.model.Advertisement;
import com.lankaspot.server.repository.AdvertisementRepository;
import com.lankaspot.server.service.kafka.AnalyticsProducer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AnalyticsIngestionService {
    private static final String REDIS_PREFIX = "lankaspot:analytics:";

    private final AdvertisementRepository advertisementRepository;
    private final VisitorIdentityService visitorIdentityService;
    private final CookieConsentService consentService;
    private final AnalyticsProducer producer;
    private final StringRedisTemplate redis;

    public void recordView(UUID advertisementRef, Authentication authentication,
                           HttpServletRequest request, HttpServletResponse response) {

        if (!consentService.analyticsAllowed(authentication, request)) return;
        VisitorIdentityService.ResolvedVisitor visitor = visitorIdentityService.resolve(authentication, request, response);
        Advertisement ad = eligibleAdvertisement(advertisementRef);
        publish(new AnalyticsEventMessage(UUID.randomUUID(), ad.getId(),
                AnalyticsEventType.ADVERTISEMENT_VIEWED, visitor.visitorKeyHash(), null, Instant.now(), null));
    }

    public EngagementStartResponse startEngagement(UUID advertisementRef, Authentication authentication,
                                                    HttpServletRequest request, HttpServletResponse response) {
        if (!consentService.analyticsAllowed(authentication, request)) {
            throw new BadRequestException("ANALYTICS_CONSENT_REQUIRED", "Analytics consent is required for engagement tracking");
        }
        Advertisement ad = eligibleAdvertisement(advertisementRef);
        visitorIdentityService.resolve(authentication, request, response);
        String sessionId = UUID.randomUUID().toString();
        long now = Instant.now().getEpochSecond();
        redis.opsForValue().set(REDIS_PREFIX + "engagement:" + sessionId,
                ad.getId() + ":0:" + now, 30, TimeUnit.MINUTES);
        return new EngagementStartResponse(sessionId);
    }

    public void heartbeat(UUID advertisementRef, String sessionId, int intervalSeconds,
                          Authentication authentication, HttpServletRequest request,
                          HttpServletResponse response) {
        if (!consentService.analyticsAllowed(authentication, request)) return;
        Advertisement ad = eligibleAdvertisement(advertisementRef);
        if (intervalSeconds < 1 || intervalSeconds > 30) {
            throw new BadRequestException("INVALID_HEARTBEAT_INTERVAL", "Heartbeat interval must be between 1 and 30 seconds");
        }

        String key = REDIS_PREFIX + "engagement:" + sessionId;
        String value = redis.opsForValue().get(key);
        if (value == null) throw new BadRequestException("ENGAGEMENT_SESSION_NOT_FOUND", "Engagement session is no longer active");

        String[] parts = value.split(":");
        if (parts.length != 3 || Long.parseLong(parts[0]) != ad.getId()) {
            throw new BadRequestException("ENGAGEMENT_SESSION_OWNER_MISMATCH", "Engagement session does not belong to this advertisement");
        }

        long lastHeartbeat = Long.parseLong(parts[2]);
        long now = Instant.now().getEpochSecond();
        long elapsed = now - lastHeartbeat;
        if (elapsed < 1 || elapsed > 35) {
            throw new BadRequestException("INVALID_HEARTBEAT_FREQUENCY", "Heartbeat timing is invalid");
        }

        int countedSeconds = Math.min(intervalSeconds, 30);
        long accumulated = Long.parseLong(parts[1]) + countedSeconds;
        redis.opsForValue().set(key, ad.getId() + ":" + accumulated + ":" + now, 30, TimeUnit.MINUTES);
    }

    public void completeEngagement(UUID advertisementRef, String sessionId,
                                   Authentication authentication, HttpServletRequest request,
                                   HttpServletResponse response) {
        if (!consentService.analyticsAllowed(authentication, request)) return;
        Advertisement ad = eligibleAdvertisement(advertisementRef);
        VisitorIdentityService.ResolvedVisitor visitor = visitorIdentityService.resolve(authentication, request, response);

        String key = REDIS_PREFIX + "engagement:" + sessionId;
        String value = redis.opsForValue().get(key);
        if (value == null) throw new BadRequestException("ENGAGEMENT_SESSION_NOT_FOUND", "Engagement session is no longer active");

        String completionKey = REDIS_PREFIX + "completion:" + sessionId;
        Boolean newlyCompleted = redis.opsForValue().setIfAbsent(completionKey, "1", 30, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(newlyCompleted)) {
            throw new BadRequestException("ENGAGEMENT_ALREADY_COMPLETED", "Engagement session was already completed");
        }

        String[] parts = value.split(":");
        if (parts.length != 3 || Long.parseLong(parts[0]) != ad.getId()) {
            throw new BadRequestException("ENGAGEMENT_SESSION_OWNER_MISMATCH", "Engagement session does not belong to this advertisement");
        }

        long duration = Math.min(Long.parseLong(parts[1]), 30 * 60);
        redis.delete(key);
        publish(new AnalyticsEventMessage(UUID.randomUUID(), ad.getId(),
                AnalyticsEventType.ADVERTISEMENT_ENGAGEMENT_COMPLETED,
                visitor.visitorKeyHash(), sessionId, Instant.now(), (int) duration));
    }

    public void recordContactRetrieval(UUID advertisementRef, Authentication authentication,
                                       HttpServletRequest request, HttpServletResponse response) {
        Advertisement ad = eligibleAdvertisement(advertisementRef);
        if (!consentService.analyticsAllowed(authentication, request)) return;
        VisitorIdentityService.ResolvedVisitor visitor = visitorIdentityService.resolve(authentication, request, response);
        publish(new AnalyticsEventMessage(UUID.randomUUID(), ad.getId(),
                AnalyticsEventType.CONTACT_RETRIEVED, visitor.visitorKeyHash(), null, Instant.now(), null));
    }

    private Advertisement eligibleAdvertisement(UUID advertisementRef) {
        Advertisement ad = advertisementRepository.findByAdvertisementRef(advertisementRef)
                .orElseThrow(() -> new AdvertisementNotFoundException(advertisementRef.toString()));
        if (ad.getExpiresAt() != null && ad.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new AdvertisementNotFoundException(advertisementRef.toString());
        }
        if (ad.getStatus() != com.lankaspot.server.enums.AdvertisementStatus.ACTIVE
                && ad.getStatus() != com.lankaspot.server.enums.AdvertisementStatus.APPROVED) {
            throw new BadRequestException("ADVERTISEMENT_NOT_ELIGIBLE", "Advertisement is not eligible for public analytics");
        }
        return ad;
    }

    private void publish(AnalyticsEventMessage event) {
        producer.publish(event);
    }
}

