
package com.lankaspot.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvertisementCacheService {

    private static final Duration QUOTE_DETAILS_TTL =
            Duration.ofMinutes(10);

    private static final Duration ADVERTISEMENT_LIST_TTL =
            Duration.ofMinutes(2);

    private static final String QUOTE_DETAILS_KEY =
            "lankaspot:advertiser:%s:quote:%d:details";

    private static final String ADVERTISEMENT_LIST_KEY =
            "lankaspot:advertiser:%s:ads:page:%d:size:%d";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;


    public void invalidateAdvertisementCache(
            UUID advertiserId,
            Long quoteId
    ) {
        if (quoteId != null) {
            evictQuoteDetails(advertiserId, quoteId);
        }

        if (advertiserId != null) {
            evictAllAdvertisementLists(advertiserId);
        }
    }

    // ----------------------------------------------------------------
    // Quote details
    // ----------------------------------------------------------------

    public <T> T getQuoteDetails(
            UUID advertiserId,
            Long quoteId,
            TypeReference<T> type
    ) {
        return get(
                quoteDetailsKey(advertiserId, quoteId),
                type
        );
    }

    public void putQuoteDetails(
            UUID advertiserId,
            Long quoteId,
            Object data
    ) {
        put(
                quoteDetailsKey(advertiserId, quoteId),
                data,
                QUOTE_DETAILS_TTL
        );
    }

    public void evictQuoteDetails(
            UUID advertiserId,
            Long quoteId
    ) {
        evict(quoteDetailsKey(advertiserId, quoteId));
    }

    // ----------------------------------------------------------------
    // Advertiser advertisement pages
    // ----------------------------------------------------------------

    public <T> T getAdvertisementList(
            UUID advertiserId,
            int page,
            int size,
            TypeReference<T> type
    ) {
        return get(
                advertisementListKey(advertiserId, page, size),
                type
        );
    }

    public void putAdvertisementList(
            UUID advertiserId,
            int page,
            int size,
            Object data
    ) {
        put(
                advertisementListKey(advertiserId, page, size),
                data,
                ADVERTISEMENT_LIST_TTL
        );
    }

    public void evictAdvertisementList(
            UUID advertiserId,
            int page,
            int size
    ) {
        evict(advertisementListKey(advertiserId, page, size));
    }

    /**
     * Invalidates every cached advertisement page for an advertiser.
     * Use after the database transaction commits.
     */
    public void evictAllAdvertisementLists(UUID advertiserId) {
        String pattern = "lankaspot:advertiser:"
                + advertiserId
                + ":ads:page:*";

        try {
            var keys = redisTemplate.keys(pattern);

            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception exception) {
            log.warn(
                    "Failed to invalidate advertisement-list cache "
                            + "for advertiser {}: {}",
                    advertiserId,
                    exception.getMessage()
            );
        }
    }

    // ----------------------------------------------------------------
    // Shared cache operations
    // ----------------------------------------------------------------

    private <T> T get(
            String key,
            TypeReference<T> type
    ) {
        try {
            String cachedValue =
                    redisTemplate.opsForValue().get(key);

            if (cachedValue == null) {
                return null;
            }

            return objectMapper.readValue(cachedValue, type);

        } catch (Exception exception) {
            log.warn(
                    "Cache read failed for key {}: {}",
                    key,
                    exception.getMessage()
            );

            evict(key);
            return null;
        }
    }

    private void put(
            String key,
            Object data,
            Duration ttl
    ) {
        try {
            String json = objectMapper.writeValueAsString(data);

            redisTemplate.opsForValue().set(key, json, ttl);

        } catch (Exception exception) {
            log.warn(
                    "Cache write failed for key {}: {}",
                    key,
                    exception.getMessage()
            );
        }
    }

    private void evict(String key) {
        try {
            redisTemplate.delete(key);

        } catch (Exception exception) {
            log.warn(
                    "Cache eviction failed for key {}: {}",
                    key,
                    exception.getMessage()
            );
        }
    }

    private String quoteDetailsKey(
            UUID advertiserId,
            Long quoteId
    ) {
        return QUOTE_DETAILS_KEY.formatted(advertiserId, quoteId);
    }

    private String advertisementListKey(
            UUID advertiserId,
            int page,
            int size
    ) {
        return ADVERTISEMENT_LIST_KEY.formatted(
                advertiserId,
                page,
                size
        );
    }
}
