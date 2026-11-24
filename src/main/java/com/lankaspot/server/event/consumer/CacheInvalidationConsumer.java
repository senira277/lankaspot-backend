package com.lankaspot.server.event.consumer;

import com.lankaspot.server.event.model.cacheInvalidation.CacheInvalidationEvent;
import com.lankaspot.server.service.AdvertisementCacheService;
import com.lankaspot.server.service.ListingsService;
import com.lankaspot.server.repository.AdQuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationConsumer {
    private final AdvertisementCacheService advertisementCacheService;
    private final ListingsService listingsService;
    private final AdQuoteRepository adQuoteRepository;
    public void consume(CacheInvalidationEvent event) {
        log.info("consuming cache invalidation Event: {}", event);
        //invalidate cache
        advertisementCacheService.invalidateAdvertisementCache(event.body().advertiserId(),event.body().quoteId());
        listingsService.incrementListingVersion();

        if (event.body().eventType()
                == com.lankaspot.server.enums.CacheInvalidationEventType.ALL_ADVERTISEMENTS_CACHE_INVALIDATED) {
            listingsService.invalidateAllAdvertisementCaches();
            return;
        }

        if (event.body().eventType()
                == com.lankaspot.server.enums.CacheInvalidationEventType.TOP_AD_UPDATED
                || event.body().eventType()
                == com.lankaspot.server.enums.CacheInvalidationEventType.TOP_AD_DELETED) {
            listingsService.refreshTopAdCache();
            return;
        }

        UUID advertisementRef = event.body().advertisementRef();
        boolean deleted = event.body().eventType()
                == com.lankaspot.server.enums.CacheInvalidationEventType.ADVERTISEMENT_FEATURES_TIERS_DELETED
                || event.body().eventType()
                == com.lankaspot.server.enums.CacheInvalidationEventType.ADVERTISEMENT_FEATURE_DELETED;

        if (advertisementRef == null && event.body().quoteId() != null) {
            adQuoteRepository.findById(event.body().quoteId()).ifPresent(quote -> {
                if (quote.getAdvertisementRef() != null) {
                    applyFeatureAndTierCache(quote.getAdvertisementRef(), deleted);
                }
            });
        } else if (advertisementRef != null) {
            applyFeatureAndTierCache(advertisementRef, deleted);
        }
    }

    private void applyFeatureAndTierCache(
            UUID advertisementRef,
            boolean deleted
    ) {
        if (deleted) {
            listingsService.deleteFeatureCache(advertisementRef);
            listingsService.deleteTierCache(advertisementRef);
        } else {
            listingsService.refreshFeatureCache(advertisementRef);
            listingsService.refreshTierCache(advertisementRef);
        }
    }
}
