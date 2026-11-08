package com.lankaspot.server.event.consumer;

import com.lankaspot.server.event.model.cacheInvalidation.CacheInvalidationEvent;
import com.lankaspot.server.service.AdvertisementCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationConsumer {
    private final AdvertisementCacheService advertisementCacheService;
    public void consume(CacheInvalidationEvent event) {
        log.info("consuming cache invalidation Event: {}", event);
        //invalidate cache
        advertisementCacheService.invalidateAdvertisementCache(event.body().advertiserId(),event.body().quoteId());
    }
}
