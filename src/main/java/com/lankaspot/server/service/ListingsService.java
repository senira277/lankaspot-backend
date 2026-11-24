package com.lankaspot.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lankaspot.server.dto.advertiser.advertisement.MediaSummaryDTO;
import com.lankaspot.server.dto.advertiser.advertisement.FunctionalGroupDTO;
import com.lankaspot.server.dto.analytics.EngagementStartResponse;
import com.lankaspot.server.dto.common.ContactDTO;
import com.lankaspot.server.dto.common.TierDTO;
import com.lankaspot.server.dto.listings.*;
import com.lankaspot.server.model.LiveAdCard;
import com.lankaspot.server.model.LiveTopAd;
import com.lankaspot.server.enums.LiveTopAdStatus;
import com.lankaspot.server.repository.LiveAdCardRepository;
import com.lankaspot.server.repository.LiveTopAdRepository;
import com.lankaspot.server.response.LankaspotResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
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
    private static final String TOP_AD_ALL_KEY = "lankaspot:top-ads:all";
    private static final String TOP_AD_CATEGORY_KEY = "lankaspot:top-ads:category:%d";
    private static final Duration LISTING_TTL = Duration.ofSeconds(60);
    private static final Duration EMPTY_LISTING_TTL = Duration.ofSeconds(20);
    private final LiveAdCardRepository liveAdCardRepository;
    private final LiveTopAdRepository liveTopAdRepository;
    private final PropertyService propertyService;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private static final Set<String> SENDABLE_TIERS = Set.of("URGENT", "TOP_AD");
    private final AdvertisementService advertisementService;
    private final AnalyticsIngestionService analyticsIngestionService;

    public GetAllListingsResponse getAllListings() {
        return getAllListings(null, null, null, null, null, null, 0, 20);
    }


    public GetAllListingsResponse getAllListings(
            Long functionalGroupId,
            Long categoryId,
            Long cityId,
            Long districtId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size
    ) {
        log.info(
                "getAllListings(functionalGroupId={}, categoryId={}, cityId={}, districtId={}, minPrice={}, maxPrice={}, page={}, size={})",
                functionalGroupId, categoryId, cityId, districtId, minPrice, maxPrice, page, size
        );

        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 100);

        String filterHash = hash(
                canonical(functionalGroupId, categoryId, cityId, districtId, minPrice, maxPrice, page, size)
        );

        long version = currentVersion();
        String key = LISTING_KEY.formatted(version, filterHash);

        GetAllListingsResponse cached = read(key);
        if (cached != null) {
            return cached;
        }

        LocalDateTime now = LocalDateTime.now();
        List<UUID> topIds = topAdIds(categoryId);
        List<LiveAdCard> topCards = topIds.isEmpty()
                ? List.of()
                : liveAdCardRepository.findForTopAds(
                        topIds, functionalGroupId, categoryId, cityId, districtId,
                        minPrice, maxPrice, now);
        Map<UUID, LiveAdCard> topCardById = topCards.stream()
                .collect(Collectors.toMap(LiveAdCard::getAdvertisementId, card -> card));
        List<LiveAdCard> orderedTopCards = topIds.stream()
                .map(topCardById::get)
                .filter(Objects::nonNull)
                .toList();
        List<UUID> excludedTopIds = orderedTopCards.stream()
                .map(LiveAdCard::getAdvertisementId)
                .toList();

        int offset = page * size;
        int topSkip = Math.min(offset, orderedTopCards.size());
        int topEnd = Math.min(topSkip + size, orderedTopCards.size());
        List<LiveAdCard> pageTopCards = orderedTopCards.subList(topSkip, topEnd);
        int remaining = size - pageTopCards.size();
        int normalOffset = offset >= orderedTopCards.size()
                ? offset - orderedTopCards.size()
                : 0;

        Page<LiveAdCard> normalCards = excludedTopIds.isEmpty()
                ? liveAdCardRepository.search(
                        functionalGroupId, categoryId, cityId, districtId, minPrice, maxPrice, now,
                        PageRequest.of(normalOffset, remaining == 0 ? 1 : remaining))
                : liveAdCardRepository.searchExcluding(
                        excludedTopIds,
                        functionalGroupId, categoryId, cityId, districtId, minPrice, maxPrice, now,
                        PageRequest.of(normalOffset, remaining == 0 ? 1 : remaining));

        List<LiveAdCard> pageCards = new ArrayList<>(pageTopCards);
        if (remaining > 0) {
            pageCards.addAll(normalCards.getContent());
        }

        Map<UUID, List<ListingFeatureDTO>> features = featuresFor(pageCards);
        Map<UUID, List<TierDTO>> tiers = tiersFor(pageCards);
        ListingSummaryDTO[] listings = pageCards.stream()
                .map(card -> toListing(
                        card,
                        features.getOrDefault(card.getAdvertisementId(), List.of()),
                        tiers.getOrDefault(card.getAdvertisementId(), List.of()).stream()
                                .filter(tier -> SENDABLE_TIERS.contains(tier.code()))
                                .toList()))
                .toArray(ListingSummaryDTO[]::new);

        long normalTotal = normalCards.getTotalElements();
        long totalElements = orderedTopCards.size() + normalTotal;
        int totalPages = (int) ((totalElements + size - 1) / size);
        GetAllListingsResponse result = new GetAllListingsResponse(
                listings, totalElements, totalPages, page, size);

        // Do not publish a result that was assembled under a stale generation.
        if (currentVersion() == version) {
            write(
                    key,
                    result,
                    listings.length == 0 ? EMPTY_LISTING_TTL : LISTING_TTL
            );
        }

        return result;
    }

    public LankaspotResponse<GetFullAdvertisement> getFullAdvertisement(UUID advertisementRef, Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        log.info("getFullAdvertisement(advertisementRef={})", advertisementRef);
        GetFullAdvertisement advertisement = advertisementService.getFullAdvertisementWithoutContact(advertisementRef.toString());
        //analytics
        analyticsIngestionService.recordView(advertisementRef, authentication, request, response);
        return LankaspotResponse.success(advertisement, "Full advertisement retrieved");
    }

    public LankaspotResponse<EngagementStartResponse> startAdEngagement(UUID advertisementRef, Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        log.info("startAdEngagement(advertisementRef={})", advertisementRef);
        EngagementStartResponse engagementStartResponse = analyticsIngestionService.startEngagement(advertisementRef, authentication, request, response);
        return LankaspotResponse.success(engagementStartResponse, "Engagement started");
    }

    public LankaspotResponse<Void> recordAdHeartbeat(UUID advertisementRef, String sessionId, int intervalSeconds, Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        log.info("recordAdHeartbeat(advertisementRef={}, sessionId={}, intervalSeconds={})", advertisementRef, sessionId, intervalSeconds);
        analyticsIngestionService.heartbeat(advertisementRef, sessionId, intervalSeconds, authentication, request, response);
        return LankaspotResponse.success(null, "Heartbeat recorded");
    }

    public LankaspotResponse<Void> completeAdEngagement(UUID advertisementRef, String sessionId, Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        log.info("completeAdEngagement(advertisementRef={}, sessionId={})", advertisementRef, sessionId);
        analyticsIngestionService.completeEngagement(advertisementRef, sessionId, authentication, request, response);
        return LankaspotResponse.success(null, "Engagement completed");
    }

    public LankaspotResponse<GetAllContactsOfListing> retrieveAdContact(UUID advertisementRef, Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        log.info("retrieveAdContact(advertisementRef={})", advertisementRef);
        List<ContactDTO> contacts = advertisementService.getAllContactsByAdvertisementRef(advertisementRef.toString());
        analyticsIngestionService.recordContactRetrieval(advertisementRef, authentication, request, response);
        return LankaspotResponse.success(new GetAllContactsOfListing(contacts), "Contact retrieved successfully");
    }

    public long incrementListingVersion() {
        Long version = redis.opsForValue().increment(LISTING_VERSION_KEY);
        return version == null ? 1L : version;
    }

    public void invalidateAllAdvertisementCaches() {
        deleteKeysByPattern("lankaspot:ad:features:*");
        deleteKeysByPattern("lankaspot:ad:tiers:*");
        deleteKeysByPattern("lankaspot:top-ads:*");
        incrementListingVersion();
        refreshTopAdCache();
    }

    private void deleteKeysByPattern(String pattern) {
        Set<String> keys = redis.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    public void refreshTopAdCache() {
        LocalDateTime now = LocalDateTime.now();
        List<LiveTopAd> active = liveTopAdRepository
                .findByStatusAndExpireAtAfterOrderByCreatedAtAsc(
                        LiveTopAdStatus.ACTIVE, now);

        int maxPerCategory = maxActiveTopAdsPerCategory();
        Map<Long, List<LiveTopAd>> byCategory = active.stream()
                .collect(Collectors.groupingBy(
                        topAd -> topAd.getCategory().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        Set<String> oldCategoryKeys = redis.keys("lankaspot:top-ads:category:*");
        if (oldCategoryKeys != null && !oldCategoryKeys.isEmpty()) {
            redis.delete(oldCategoryKeys);
        }
        redis.delete(TOP_AD_ALL_KEY);

        Map<Long, List<LiveTopAd>> limited = new LinkedHashMap<>();
        for (Map.Entry<Long, List<LiveTopAd>> entry : byCategory.entrySet()) {
            List<LiveTopAd> categoryTopAds = entry.getValue().stream()
                    .sorted(Comparator
                            .comparing(LiveTopAd::getCreatedAt,
                                    Comparator.nullsLast(Comparator.naturalOrder())))
                    .limit(maxPerCategory)
                    .toList();
            if (!categoryTopAds.isEmpty()) limited.put(entry.getKey(), categoryTopAds);
        }

        for (Map.Entry<Long, List<LiveTopAd>> entry : limited.entrySet()) {
            String key = TOP_AD_CATEGORY_KEY.formatted(entry.getKey());
            addTopAdsToRedis(key, entry.getValue());
        }

        List<LiveTopAd> global = limited.values().stream()
                .flatMap(List::stream)
                .sorted(Comparator
                        .comparing(LiveTopAd::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        addTopAdsToRedis(TOP_AD_ALL_KEY, global);

    }

    public void invalidateTopAdCache() {
        refreshTopAdCache();
        incrementListingVersion();
    }

    private int maxActiveTopAdsPerCategory() {
        try {
            String configured = propertyService.getValue("MAX_ACTIVE_TOP_ADS_PER_CATEGORY");
            if (configured != null) {
                int value = Integer.parseInt(configured.trim());
                if (value >= 0) return value;
            }
        } catch (Exception e) {
            log.warn("Could not read MAX_ACTIVE_TOP_ADS_PER_CATEGORY", e);
        }
        return 5;
    }

    private void addTopAdsToRedis(String key, List<LiveTopAd> topAds) {
        if (topAds.isEmpty()) {
            redis.expire(key, Duration.ofSeconds(20));
            return;
        }
        for (LiveTopAd topAd : topAds) {
            if (topAd.getCreatedAt() == null) continue;
            redis.opsForZSet().add(
                    key,
                    topAd.getAdvertisement().getAdvertisementRef().toString(),
                    topAd.getCreatedAt()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli());
        }
        LocalDateTime earliestExpiry = topAds.stream()
                .map(LiveTopAd::getExpireAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().plusSeconds(20));
        long ttlSeconds = Math.max(
                1,
                java.time.Duration.between(LocalDateTime.now(), earliestExpiry).getSeconds());
        redis.expire(key, Duration.ofSeconds(ttlSeconds));
    }

    private List<UUID> topAdIds(Long categoryId) {
        String key = categoryId == null
                ? TOP_AD_ALL_KEY
                : TOP_AD_CATEGORY_KEY.formatted(categoryId);
        try {
            if (Boolean.FALSE.equals(redis.hasKey(key))) {
                refreshTopAdCache();
            }
            Set<String> ids = redis.opsForZSet().range(key, 0, -1);
            if (ids != null && !ids.isEmpty()) {
                return ids.stream()
                        .map(UUID::fromString)
                        .limit(maxActiveTopAdsPerCategory())
                        .toList();
            }
        } catch (Exception e) {
            log.warn("Could not read top-ad Redis cache; falling back to PostgreSQL", e);
        }

        LocalDateTime now = LocalDateTime.now();
        int limit = maxActiveTopAdsPerCategory();
        return liveTopAdRepository
                .findByStatusAndExpireAtAfterOrderByCreatedAtAsc(
                        LiveTopAdStatus.ACTIVE, now)
                .stream()
                .filter(topAd -> categoryId == null
                        || Objects.equals(topAd.getCategory().getId(), categoryId))
                .sorted(Comparator.comparing(
                        LiveTopAd::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(limit)
                .map(topAd -> topAd.getAdvertisement().getAdvertisementRef())
                .toList();
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

    private ListingSummaryDTO toListing(LiveAdCard c, List<ListingFeatureDTO> features, List<TierDTO> tiers) {
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