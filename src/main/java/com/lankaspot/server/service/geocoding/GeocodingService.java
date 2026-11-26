package com.lankaspot.server.service.geocoding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lankaspot.server.dto.location.GeoapifyGeocodingResponse;
import com.lankaspot.server.geocoding.GeocodingClient;
import com.lankaspot.server.response.LankaspotResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeocodingService {
    private static final Duration AUTOCOMPLETE_CACHE_TTL = Duration.ofDays(30);
    private static final String AUTOCOMPLETE_CACHE_KEY =
            "lankaspot:geocoding:autocomplete:%s";
    private final GeocodingClient geocodingClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public LankaspotResponse<GeoapifyGeocodingResponse> autocomplete(String text) {
        String normalizedText = text == null
                ? ""
                : text.trim().toLowerCase(Locale.ROOT);
        String cacheKey = AUTOCOMPLETE_CACHE_KEY.formatted(normalizedText);

        try {
            String cachedValue = redisTemplate.opsForValue().get(cacheKey);


            if (cachedValue != null) {
                log.info("Autocomplete cache hit for key {}", cacheKey);
                try {
                    GeoapifyGeocodingResponse cachedResponse =
                            objectMapper.readValue(
                                    cachedValue,
                                    GeoapifyGeocodingResponse.class
                            );

                    return LankaspotResponse.success(
                            cachedResponse,
                            "Autocomplete results fetched successfully"
                    );
                } catch (Exception exception) {
                    log.warn(
                            "Failed to deserialize autocomplete cache for key {}: {}",
                            cacheKey,
                            exception.getMessage()
                    );
                    evictAutocompleteCache(cacheKey);
                }
            }
        } catch (Exception exception) {
            log.warn(
                    "Cache read failed for key {}: {}",
                    cacheKey,
                    exception.getMessage()
            );
        }
        log.info("Autocomplete cache miss for key {}", cacheKey);
        log.info("Fetching autocomplete results from Geoapify for text: {}", normalizedText);
        GeoapifyGeocodingResponse result =
                geocodingClient.autocomplete(normalizedText);

        if (result != null) {
            try {
                String json = objectMapper.writeValueAsString(result);
                redisTemplate.opsForValue().set(
                        cacheKey,
                        json,
                        AUTOCOMPLETE_CACHE_TTL
                );
            } catch (Exception exception) {
                log.warn(
                        "Cache write failed for key {}: {}",
                        cacheKey,
                        exception.getMessage()
                );
            }
        }

        return LankaspotResponse.success(
                result,
                "Autocomplete results fetched successfully"
        );
    }

    private void evictAutocompleteCache(String cacheKey) {
        try {
            redisTemplate.delete(cacheKey);
        } catch (Exception exception) {
            log.warn(
                    "Cache eviction failed for key {}: {}",
                    cacheKey,
                    exception.getMessage()
            );
        }
    }
}
