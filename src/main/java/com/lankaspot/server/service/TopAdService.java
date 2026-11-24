package com.lankaspot.server.service;

import com.lankaspot.server.enums.LiveTopAdStatus;
import com.lankaspot.server.model.Advertisement;
import com.lankaspot.server.model.Category;
import com.lankaspot.server.model.LiveTopAd;
import com.lankaspot.server.repository.LiveTopAdRepository;
import com.lankaspot.server.enums.CacheInvalidationEventType;
import com.lankaspot.server.event.model.cacheInvalidation.CacheInvalidationBody;
import com.lankaspot.server.service.kafka.CacheInvalidationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TopAdService {
    private final LiveTopAdRepository liveTopAdRepository;
    private final PropertyService propertyService;
    private final CacheInvalidationProducer cacheInvalidationProducer;



    public void createLiveTopAd(Advertisement advertisement) {
        LiveTopAd existing = liveTopAdRepository
                .findByAdvertisement_AdvertisementRefAndStatus(
                        advertisement.getAdvertisementRef(), LiveTopAdStatus.ACTIVE)
                .orElse(null);
        if (existing != null && existing.getExpireAt().isAfter(LocalDateTime.now())) {
            return;
        }
        if (!isSpotAvailable(advertisement.getCategory())) {
            throw new IllegalStateException("No available spot for a new top ad");
        }
        LiveTopAd liveTopAd = new LiveTopAd();

        liveTopAd.setAdvertisement(advertisement);
        liveTopAd.setCategory(advertisement.getCategory());
        liveTopAd.setStatus(LiveTopAdStatus.ACTIVE);
        liveTopAd.setExpireAt(LocalDateTime.now().plusDays(propertyService.getValue("TOP_AD_DURATION_DAYS") != null ? Long.parseLong(propertyService.getValue("TOP_AD_DURATION_DAYS")) : 7));

        liveTopAdRepository.save(liveTopAd);
        publishTopAdCacheEvent(advertisement, CacheInvalidationEventType.TOP_AD_UPDATED);
        log.info("Created LiveTopAd for Advertisement ID: {}", advertisement.getId());
    }

    public boolean isSpotAvailable(Category category) {
        int MAX_ACTIVE_TOP_ADS = Integer.parseInt(propertyService.getValue("MAX_ACTIVE_TOP_ADS_PER_CATEGORY") != null ? propertyService.getValue("MAX_ACTIVE_TOP_ADS_PER_CATEGORY") : "3");
        LocalDateTime now = LocalDateTime.now();
        long activeCount = liveTopAdRepository.countByCategoryAndStatusAndExpireAtAfter(
                category, LiveTopAdStatus.ACTIVE, now);
        return activeCount < MAX_ACTIVE_TOP_ADS;
    }

//    @Scheduled(fixedDelayString = "${lankaspot.top-ad.expiry-check-ms:60000}")
//    public void expireExpiredTopAds() {
//        LocalDateTime now = LocalDateTime.now();
//        List<LiveTopAd> expired = liveTopAdRepository
//                .findByStatusAndExpireAtLessThanEqual(LiveTopAdStatus.ACTIVE, now);
//        if (expired.isEmpty()) return;
//        expired.forEach(topAd -> topAd.setStatus(LiveTopAdStatus.EXPIRED));
//        liveTopAdRepository.saveAll(expired);
//
//        cacheInvalidationProducer.publish(new CacheInvalidationBody(
//                CacheInvalidationEventType.TOP_AD_DELETED,
//                null,
//                null,
//                0L,
//                null
//        ));
//    }

    private void publishTopAdCacheEvent(
            Advertisement advertisement,
            CacheInvalidationEventType eventType
    ) {
        CacheInvalidationBody body = new CacheInvalidationBody(
                eventType,
                null,
                null,
                0L,
                advertisement.getAdvertisementRef()
        );
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            cacheInvalidationProducer.publish(body);
                        }
                    });
        } else {
            cacheInvalidationProducer.publish(body);
        }
    }

}
