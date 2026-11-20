package com.lankaspot.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lankaspot.server.dto.advertiser.advertisement.MediaSummaryDTO;
import com.lankaspot.server.dto.advertiser.advertisement.FunctionalGroupDTO;
import com.lankaspot.server.dto.common.TierDTO;
import com.lankaspot.server.dto.listings.*;
import com.lankaspot.server.model.LiveAdCard;
import com.lankaspot.server.repository.LiveAdCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ListingsService {
    private record CachedTierDTO(Long id, String code, LocalDateTime expiresAt) {
    }

    public static final String LISTING_VERSION_KEY = "lankaspot:public:listings:version";
    private static final String LISTING_KEY = "lankaspot:public:listings:v%s:%s";
    private static final String FEATURE_KEY = "lankaspot:ad:features:%s";
    private static final String TIER_KEY = "lankaspot:ad:tiers:%s";
    private static final Duration LISTING_TTL = Duration.ofSeconds(60);
    private static final Duration EMPTY_LISTING_TTL = Duration.ofSeconds(20);
    private final LiveAdCardRepository liveAdCardRepository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public GetAllListingsResponse getAllListings() {
        return getAllListings(null, null, null, null, null, 0, 20);
    }

    public GetAllListingsResponse getAllListings(Long categoryId, Long cityId, Long districtId,
                                                  BigDecimal minPrice, BigDecimal maxPrice,
                                                  int page, int size) {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 100);
        String filterHash = hash(canonical(categoryId, cityId, districtId, minPrice, maxPrice, page, size));
        long version = currentVersion();
        String key = LISTING_KEY.formatted(version, filterHash);
        GetAllListingsResponse cached = read(key);
        if (cached != null) return cached;

        Page<LiveAdCard> cards = liveAdCardRepository.search(
                categoryId, cityId, districtId, minPrice, maxPrice,
                LocalDateTime.now(), PageRequest.of(page, size));
        Map<UUID, List<ListingFeatureDTO>> features = featuresFor(cards.getContent());
        Map<UUID, List<TierDTO>> tiers = tiersFor(cards.getContent());
        ListingSummaryDTO[] listings = cards.getContent().stream()
                .map(card -> toListing(card,
                        features.getOrDefault(card.getAdvertisementId(), List.of()),
                        tiers.getOrDefault(card.getAdvertisementId(), List.of())))
                .toArray(ListingSummaryDTO[]::new);
        GetAllListingsResponse result = new GetAllListingsResponse(
                listings, cards.getTotalElements(), cards.getTotalPages(), page, size);
        // Do not publish a result that was assembled under a stale generation.
        if (currentVersion() == version) write(key, result,
                listings.length == 0 ? EMPTY_LISTING_TTL : LISTING_TTL);
        return result;
    }

    public long incrementListingVersion() {
        Long version = redis.opsForValue().increment(LISTING_VERSION_KEY);
        return version == null ? 1L : version;
    }

    public void refreshFeatureCache(UUID advertisementId) {
        List<ListingFeatureDTO> rows = liveAdCardRepository.findFeaturesByAdvertisementIds(List.of(advertisementId))
                .stream().map(f -> new ListingFeatureDTO(f.name(), f.value(), f.unit())).toList();
        try {
            redis.opsForValue().set(FEATURE_KEY.formatted(advertisementId),
                    objectMapper.writeValueAsString(rows), Duration.ofDays(30));
        } catch (Exception e) {
            throw new IllegalStateException("Could not update feature cache for " + advertisementId, e);
        }
    }

    public void deleteFeatureCache(UUID advertisementId) {
        redis.delete(FEATURE_KEY.formatted(advertisementId));
    }

    public void refreshTierCache(UUID advertisementId) {
        refreshTierCache(advertisementId, LocalDateTime.now());
    }

    public void deleteTierCache(UUID advertisementId) {
        redis.delete(TIER_KEY.formatted(advertisementId));
    }

    private void refreshTierCache(UUID advertisementId, LocalDateTime now) {
        List<CachedTierDTO> activeTiers = activeTiers(
                liveAdCardRepository.findTiersByAdvertisementIds(List.of(advertisementId)), now);
        try {
            redis.opsForValue().set(TIER_KEY.formatted(advertisementId),
                    objectMapper.writeValueAsString(activeTiers),
                    tierCacheTtl(activeTiers, now));
        } catch (Exception e) {
            throw new IllegalStateException("Could not update tier cache for " + advertisementId, e);
        }
    }

    private Map<UUID, List<ListingFeatureDTO>> featuresFor(List<LiveAdCard> cards) {
        if (cards.isEmpty()) return Map.of();
        List<UUID> ids = cards.stream().map(LiveAdCard::getAdvertisementId).toList();
        List<String> keys = ids.stream().map(id -> FEATURE_KEY.formatted(id)).toList();
        List<String> values = redis.opsForValue().multiGet(keys);
        Map<UUID, List<ListingFeatureDTO>> result = new HashMap<>();
        List<UUID> missing = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            List<ListingFeatureDTO> value = deserialize(values == null ? null : values.get(i));
            if (value == null) missing.add(ids.get(i));
            else result.put(ids.get(i), value);
        }
        if (!missing.isEmpty()) {
            List<ListingFeatureDTO> rows = liveAdCardRepository.findFeaturesByAdvertisementIds(missing);
            Map<UUID, List<ListingFeatureDTO>> grouped = rows.stream().collect(Collectors.groupingBy(
                    ListingFeatureDTO::advertisementRef,
                    LinkedHashMap::new,
                    Collectors.toList()));
            Map<UUID, String> serialized = new HashMap<>();
            for (UUID id : missing) {
                List<ListingFeatureDTO> value = grouped.getOrDefault(id, List.of()).stream()
                        .map(f -> new ListingFeatureDTO(f.name(), f.value(), f.unit())).toList();
                result.put(id, value);
                try { serialized.put(id, objectMapper.writeValueAsString(value)); }
                catch (Exception e) { log.warn("Could not serialize features for {}", id, e); }
            }
            if (!serialized.isEmpty()) redis.opsForValue().multiSet(serialized.entrySet().stream()
                    .collect(Collectors.toMap(e -> FEATURE_KEY.formatted(e.getKey()), Map.Entry::getValue)));
        }
        return result;
    }

    private Map<UUID, List<TierDTO>> tiersFor(List<LiveAdCard> cards) {
        if (cards.isEmpty()) return Map.of();

        LocalDateTime now = LocalDateTime.now();
        List<UUID> ids = cards.stream().map(LiveAdCard::getAdvertisementId).toList();
        List<String> keys = ids.stream().map(id -> TIER_KEY.formatted(id)).toList();
        List<String> values = redis.opsForValue().multiGet(keys);
        Map<UUID, List<TierDTO>> result = new HashMap<>();
        List<UUID> missing = new ArrayList<>();

        for (int i = 0; i < ids.size(); i++) {
            List<CachedTierDTO> cached = deserializeTiers(values == null ? null : values.get(i));
            if (cached == null) {
                missing.add(ids.get(i));
            } else {
                result.put(ids.get(i), activeCachedTiers(cached, now).stream()
                        .map(tier -> new TierDTO(tier.id(), tier.code()))
                        .toList());
            }
        }

        if (!missing.isEmpty()) {
            List<ListingTierDTO> rows =
                    liveAdCardRepository.findTiersByAdvertisementIds(missing);
            Map<UUID, List<ListingTierDTO>> grouped = rows.stream().collect(Collectors.groupingBy(
                    ListingTierDTO::advertisementRef,
                    LinkedHashMap::new,
                    Collectors.toList()));

            for (UUID id : missing) {
                List<CachedTierDTO> active = activeTiers(grouped.getOrDefault(id, List.of()), now);
                result.put(id, active.stream()
                        .map(tier -> new TierDTO(tier.id(), tier.code()))
                        .toList());
                try {
                    redis.opsForValue().set(
                            TIER_KEY.formatted(id),
                            objectMapper.writeValueAsString(active),
                            tierCacheTtl(active, now));
                } catch (Exception e) {
                    log.warn("Could not serialize tiers for {}", id, e);
                }
            }
        }
        return result;
    }

    private List<CachedTierDTO> activeCachedTiers(List<CachedTierDTO> tiers, LocalDateTime now) {
        return tiers.stream()
                .filter(tier -> tier.expiresAt() == null || tier.expiresAt().isAfter(now))
                .toList();
    }

    private List<CachedTierDTO> activeTiers(List<ListingTierDTO> rows, LocalDateTime now) {
        return rows.stream()
                .filter(row -> row.createdAt() != null
                        && row.durationDays() != null
                        && row.durationDays() >= 0)
                .map(row -> new CachedTierDTO(
                        row.id(),
                        row.code(),
                        row.createdAt().plusDays(row.durationDays())))
                .filter(tier -> tier.expiresAt().isAfter(now))
                .sorted(Comparator.comparing(CachedTierDTO::id))
                .toList();
    }

    private List<CachedTierDTO> deserializeTiers(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<List<CachedTierDTO>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private Duration tierCacheTtl(List<CachedTierDTO> tiers, LocalDateTime now) {
        if (tiers.isEmpty()) return Duration.ofSeconds(20);
        long seconds = tiers.stream()
                .map(CachedTierDTO::expiresAt)
                .filter(Objects::nonNull)
                .mapToLong(expiresAt -> java.time.Duration.between(now, expiresAt).getSeconds())
                .min()
                .orElse(20L);
        return Duration.ofSeconds(Math.min(60L, Math.max(1L, seconds)));
    }

    private ListingSummaryDTO toListing(LiveAdCard c,
                                        List<ListingFeatureDTO> features,
                                        List<TierDTO> tiers) {
        FunctionalGroupDTO functionalGroup = c.getFunctionalGroupId() == null
                ? null
                : new FunctionalGroupDTO(c.getFunctionalGroupId(), c.getFunctionalGroupName());
        return new ListingSummaryDTO("0", c.getAdvertisementId().toString(), functionalGroup,
                new ListingQuoteDTO(c.getQuoteRef(), c.getTitle(), c.getCityName(), c.getDistrictName(),
                        c.getCategoryName(), c.getPrice(), c.getSlug(), c.getNearestLandmark(), c.getTown()),
                new MediaSummaryDTO(c.getCoverUrl(), true), features, tiers);
    }

    private List<ListingFeatureDTO> deserialize(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<List<ListingFeatureDTO>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private long currentVersion() {
        String value = redis.opsForValue().get(LISTING_VERSION_KEY);
        if (value == null) {
            Boolean created = redis.opsForValue().setIfAbsent(LISTING_VERSION_KEY, "1");
            if (Boolean.TRUE.equals(created)) return 1L;
            value = redis.opsForValue().get(LISTING_VERSION_KEY);
        }
        try { return Long.parseLong(value); }
        catch (NumberFormatException e) { redis.opsForValue().set(LISTING_VERSION_KEY, "1"); return 1L; }
    }

    private String canonical(Object... values) {
        return Arrays.stream(values).map(v -> v == null ? "" : v.toString().trim()).collect(Collectors.joining("|"));
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) { throw new IllegalStateException("Unable to hash listing cache key", e); }
    }

    private GetAllListingsResponse read(String key) {
        try {
            String json = redis.opsForValue().get(key);
            return json == null ? null
                    : objectMapper.readValue(json, new TypeReference<GetAllListingsResponse>() {});
        } catch (Exception e) {
            redis.delete(key);
            return null;
        }
    }

    private void write(String key, Object value, Duration ttl) {
        try { redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl); }
        catch (Exception e) { log.warn("Could not write listing cache {}", key, e); }
    }
}
