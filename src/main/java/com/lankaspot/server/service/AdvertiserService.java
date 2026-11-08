package com.lankaspot.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lankaspot.server.constants.NotificationConstants;
import com.lankaspot.server.dto.advertiser.advertisement.*;
import com.lankaspot.server.dto.advertiser.discount.ValidateDiscountCodeRequest;
import com.lankaspot.server.dto.advertiser.discount.ValidateDiscountCodeResponseData;
import com.lankaspot.server.dto.advertiser.retrieve.*;
import com.lankaspot.server.dto.advertiser.review.GetReviewQuoteByUserResponseData;
import com.lankaspot.server.dto.advertiser.review.ReviewQuoteByUserRequestDTO;
import com.lankaspot.server.dto.advertiser.savePropertyInfo.SaveAdvertPropertyInfoResponseData;
import com.lankaspot.server.dto.advertiser.savePropertyInfo.SavePropertyInfoRequest;
import com.lankaspot.server.dto.advertiser.subscription.*;
import com.lankaspot.server.dto.advertiser.uploads.*;
import com.lankaspot.server.event.model.cacheInvalidation.CacheInvalidationBody;
import com.lankaspot.server.enums.*;
import com.lankaspot.server.event.model.notification.NotificationEventBody;
import com.lankaspot.server.exception.*;
import com.lankaspot.server.model.*;
import com.lankaspot.server.repository.*;
import com.lankaspot.server.response.LankaspotResponse;
import com.lankaspot.server.service.kafka.CacheInvalidationProducer;
import com.lankaspot.server.service.kafka.NotificationProducer;
import com.lankaspot.server.service.reviewer.ReviewerQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdvertiserService {

    /**
     * A quote can be edited in this flow only before it is purchased. Once it becomes a policy it
     * is changed through the amendment flow instead.
     */
    private static final Set<AdQuoteStatus> EDITABLE_STATUSES =
            EnumSet.of(AdQuoteStatus.IN_COMPLETE, AdQuoteStatus.COMPLETE);
    private static final Set<AdQuoteStatus> AMENDABLE_STATUSES =
            EnumSet.of(AdQuoteStatus.REQUESTED_CHANGES, AdQuoteStatus.REJECTED);

    private static final int FEATURE_VALUE_MAX_LENGTH = 100;
    // One message for every "can't use this code" reason, so the endpoint can't be used to probe which codes exist.
    private static final String DISCOUNT_INVALID_MESSAGE = "This discount code is invalid or has expired.";
    private static final String DISCOUNT_ALREADY_USED_MESSAGE = "You have already used this discount code.";

    private final AdQuoteRepository adQuoteRepository;
    private final AdQuoteUpdateRepository adQuoteUpdateRepository;
    private final CategoryRepository categoryRepository;
    private final CityRepository cityRepository;
    private final FeatureRepository featureRepository;
    private final AdQuoteMediaRepository adQuoteMediaRepository;
    private final AssetUploaderService assetUploaderService;
    private final ContactRepository contactRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final AdvertisementTierRepository advertisementTierRepository;
    private final AdQuoteSubscriptionPlanRepository adQuoteSubscriptionPlanRepository;
    private final AdQuoteTierRepository adQuoteTierRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final DiscountCodeUsageRepository discountCodeUsageRepository;
    private final PaymentRegistryRepository paymentRegistryRepository;
    private final AdvertisementRepository advertisementRepository;
    private final AdvertisementService advertisementService;
    private final UserRepository userRepository;
    private final AdvertisementApprovalRepository advertisementApprovalRepository;
    private final AuditLogService auditLogService;
    private final ReviewerQueueService reviewerQueueService;
    private final AdvertisementCacheService advertisementCacheService;
    private final CacheInvalidationProducer cacheInvalidationProducer;
    private final NotificationProducer notificationProducer;
    private final UtilService utilService;
    private final FunctionalGroupRepository functionalGroupRepository;

    /**
     * Saves step 1.
     * <ul>
     *   <li>No quoteRef: creates a new quote (version 1).</li>
     *   <li>quoteRef set: updates that same quote in place, as long as it belongs to the caller and
     *       has not been purchased. The id (quoteRef) never changes; the version is incremented.</li>
     * </ul>
     * Business rule violations are thrown as exceptions and rendered by GlobalExceptionHandler.
     */
    @Transactional
    public LankaspotResponse<SaveAdvertPropertyInfoResponseData> savePropertyInfo(SavePropertyInfoRequest request, Authentication authentication) {
        try {
            UUID advertiserId = UUID.fromString(authentication.getName());

            log.info("savePropertyInfo started: advertiserId={}, quoteRef={}, categoryId={}, cityId={}, featureCount={}",
                    advertiserId, request.getQuoteRef(), request.getCategoryId(), request.getCityId(),
                    request.getFeatures() == null ? 0 : request.getFeatures().size());

            // Update: load the quote first, so ownership/state problems fail fast.
            AdQuote existing = request.getQuoteRef() == null
                    ? null
                    : loadEditableQuote(request.getQuoteRef(), advertiserId);

            Category category = categoryRepository.findById(request.getCategoryId())
                    .filter(Category::isActive)
                    .orElseThrow(() -> new CategoryNotAvailableException(request.getCategoryId()));

            log.info("Category resolved: id={}, name={}", category.getId(), category.getName());

            City city = cityRepository.findById(request.getCityId())
                    .filter(c -> c.isActive() && c.getDistrict().isActive())
                    .orElseThrow(() -> new CityNotAvailableException(request.getCityId()));

            log.info("City resolved: id={}", city.getId());

            List<ResolvedFeature> features = resolveFeatures(category, request.getFeatures());

            log.info("Features resolved: count={}", features.size());

            Long quoteId = existing == null
                    ? createQuote(request, advertiserId, category, city, features)
                    : updateQuote(existing, request, category, city, features);

            return LankaspotResponse.success(
                    new SaveAdvertPropertyInfoResponseData(quoteId),
                    "Property info saved successfully");

        } catch (Exception e) {
            log.error("Failed to save property info: quoteRef={}", request.getQuoteRef(), e);
            throw e;
        }
    }

    @Transactional
    public LankaspotResponse<UploadAdvertMediaResponseData> uploadMedia(Long quoteRef, Integer primaryIndex, List<MultipartFile> photos, Authentication authentication) {
        UUID advertiserId = UUID.fromString(authentication.getName());

        if (photos == null || photos.isEmpty()) {
            throw new IllegalArgumentException("At least one photo is required");
        }

        if (primaryIndex == null || primaryIndex < 0 || primaryIndex >= photos.size()) {
            throw new IllegalArgumentException("Invalid primary photo index");
        }

        AdQuote quote = adQuoteRepository.findByIdForUpdate(quoteRef)
                .filter(q -> Objects.equals(q.getAdvertiserId(), advertiserId))
                .orElseThrow(() -> new QuoteNotFoundException(quoteRef));

        if (!EDITABLE_STATUSES.contains(quote.getStatus())) {
            throw new QuoteNotEditableException(
                    quoteRef, String.valueOf(quote.getStatus())
            );
        }

        List<AdQuoteMedia> existingMedia =
                adQuoteMediaRepository.findByAdQuote_IdOrderBySortOrderAscIdAsc(
                        quoteRef
                );

        final int maxPhotos = 6;

        if (existingMedia.size() + photos.size() > maxPhotos) {
            throw new IllegalArgumentException(
                    "A maximum of " + maxPhotos + " photos is allowed per quote"
            );
        }

        List<UploadedAsset> uploadedAssets;

        try {
            uploadedAssets = assetUploaderService.uploadAssets(
                    advertiserId,
                    quoteRef,
                    MediaType.IMAGE,
                    photos
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to upload photos. Please try again.", exception
            );
        }

        try {
            // The newly selected cover replaces the old cover.
            existingMedia.forEach(media -> media.setPrimary(false));

            int nextSortOrder = existingMedia.stream()
                    .map(AdQuoteMedia::getSortOrder)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(-1) + 1;

            List<AdQuoteMedia> newMedia = new ArrayList<>();

            for (int i = 0; i < uploadedAssets.size(); i++) {
                UploadedAsset asset = uploadedAssets.get(i);

                AdQuoteMedia media = new AdQuoteMedia();
                media.setAdQuote(quote);
                media.setMediaType(asset.mediaType());
                media.setMediaStatus(MediaStatus.PENDING_REVIEW);
                media.setUrl(asset.secureUrl());
                media.setVersion(media.nextAvailableVersion());
                media.setPublicId(asset.publicId());
                media.setSortOrder(nextSortOrder + i);
                media.setPrimary(i == primaryIndex);

                newMedia.add(media);
            }

            adQuoteMediaRepository.saveAll(newMedia);
            adQuoteMediaRepository.flush();
            adQuoteRepository.updateLastCompletedStep(quoteRef, 2);

            List<UploadedAdvertMediaDTO> responseMedia =
                    adQuoteMediaRepository
                            .findByAdQuote_IdOrderBySortOrderAscIdAsc(quoteRef)
                            .stream()
                            .map(UploadedAdvertMediaDTO::from)
                            .toList();


            return LankaspotResponse.success(
                    new UploadAdvertMediaResponseData(quoteRef, responseMedia),
                    "Photos uploaded successfully"
            );
        } catch (RuntimeException exception) {
            // Database persistence failed after Cloudinary succeeded.
            // Attempt to clean up this upload batch to avoid orphaned assets.
            for (UploadedAsset asset : uploadedAssets) {
                try {
                    assetUploaderService.deleteUploadedAsset(
                            advertiserId,
                            quoteRef,
                            asset.publicId(),
                            asset.resourceType()
                    );
                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);
                    log.error(
                            "Failed to clean up uploaded asset after persistence failure. quoteRef={}",
                            quoteRef,
                            cleanupException
                    );
                }
            }

            throw exception;
        }
    }

    @Transactional
    public LankaspotResponse<RemoveAdvertMediaResponseData> requestMediaRemoval(Long quoteRef, Long mediaId, Authentication authentication) {
        UUID advertiserId = UUID.fromString(authentication.getName());

        AdQuote quote = adQuoteRepository.findByIdForUpdate(quoteRef)
                .filter(q -> Objects.equals(q.getAdvertiserId(), advertiserId))
                .orElseThrow(() -> new QuoteNotFoundException(quoteRef));

        if (!EDITABLE_STATUSES.contains(quote.getStatus())) {
            throw new QuoteNotEditableException(
                    quoteRef, String.valueOf(quote.getStatus())
            );
        }

        AdQuoteMedia media = adQuoteMediaRepository
                .findByIdAndAdQuote_Id(mediaId, quoteRef)
                .orElseThrow(() -> new IllegalArgumentException(
                        "The specified photo does not belong to this quote"
                ));

        if (media.getMediaStatus() != MediaStatus.REQUESTED_DELETION) {
            media.setMediaStatus(MediaStatus.REQUESTED_DELETION);
            adQuoteMediaRepository.save(media);
        }

        // Do not delete the Cloudinary asset here.
        // The reviewer workflow decides whether to approve the removal.
        return LankaspotResponse.success(
                new RemoveAdvertMediaResponseData(
                        quoteRef, mediaId, media.getMediaStatus()
                ),
                "Photo removal requested successfully"
        );
    }

    @Transactional
    public LankaspotResponse<SetPrimaryAdvertMediaResponseData> setPrimaryMedia(SetPrimaryAdvertMediaRequest request, Authentication authentication) {
        UUID advertiserId = UUID.fromString(authentication.getName());
        Long quoteRef = request.quoteRef();

        AdQuote quote = adQuoteRepository.findByIdForUpdate(quoteRef)
                .filter(q -> Objects.equals(q.getAdvertiserId(), advertiserId))
                .orElseThrow(() -> new QuoteNotFoundException(quoteRef));

        if (!EDITABLE_STATUSES.contains(quote.getStatus())) {
            throw new QuoteNotEditableException(
                    quoteRef, String.valueOf(quote.getStatus())
            );
        }

        List<AdQuoteMedia> mediaList =
                adQuoteMediaRepository.findAllByQuoteIdForUpdate(quoteRef);

        AdQuoteMedia selected = mediaList.stream()
                .filter(media -> Objects.equals(
                        media.getId(), request.mediaId()
                ))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "The specified photo does not belong to this quote"
                ));

        if (selected.getMediaStatus() == MediaStatus.REQUESTED_DELETION
                || selected.getMediaStatus() == MediaStatus.REJECTED) {
            throw new IllegalArgumentException(
                    "This photo cannot be selected as the cover"
            );
        }
        for (AdQuoteMedia media : mediaList) {
            media.setPrimary(false);
        }

        for (AdQuoteMedia media : mediaList) {
            media.setPrimary(Objects.equals(media.getId(), selected.getId()));
        }

        adQuoteMediaRepository.saveAll(mediaList);
        adQuoteMediaRepository.flush();

        return LankaspotResponse.success(
                new SetPrimaryAdvertMediaResponseData(
                        quoteRef, selected.getId(), true
                ),
                "Cover photo updated successfully"
        );
    }

    @Transactional(readOnly = true)
    public LankaspotResponse<GetQuoteWithMediaResponseData> getQuote(Long quoteRef, Authentication authentication) {
        UUID advertiserId = UUID.fromString(authentication.getName());

        // 1. Check Redis cache
        GetQuoteWithMediaResponseData cachedData =
                advertisementCacheService.getQuoteDetails(
                        advertiserId,
                        quoteRef,
                        new TypeReference<GetQuoteWithMediaResponseData>() {}
                );

        if (cachedData != null) {
            return LankaspotResponse.success(
                    cachedData,
                    "Quote retrieved successfully"
            );
        }

        // 2. Retrieve quote and verify ownership
        AdQuote quote = adQuoteRepository.findById(quoteRef)
                .filter(q -> Objects.equals(q.getAdvertiserId(), advertiserId))
                .orElseThrow(() -> new QuoteNotFoundException(quoteRef));

        // 3. Retrieve media
        List<MediaDefDTO> media = adQuoteMediaRepository
                .findByAdQuote_IdOrderBySortOrderAscIdAsc(quoteRef)
                .stream()
                .map(MediaDefDTO::from)
                .toList();

        // 4. Retrieve and map features
        List<FeatureDefDTO> features = quote.getFeatures().stream()
                .map(f -> new FeatureDefDTO(
                        f.getFeature().getId(),
                        f.getFeature().getName(),
                        f.getValue(),
                        f.getFeature().getUnit()
                ))
                .sorted(Comparator.comparing(FeatureDefDTO::id))
                .toList();

        // 5. Determine cover image
        String coverImage = media.stream()
                .filter(MediaDefDTO::primary)
                .findFirst()
                .map(MediaDefDTO::url)
                .orElse(null);

        // 6. Map quote DTO
        QuoteDTO quoteDto = new QuoteDTO(
                quote.getFunctionalGroup() != null ? new FunctionalGroupDTO(
                        quote.getFunctionalGroup().getId(),
                        quote.getFunctionalGroup().getName()
                ) : null,
                quote.getId(),
                quote.getId(), // quoteRef
                quote.getVersion(),
                quote.getSlug(),
                quote.getStatus(),
                quote.getSource(),
                quote.getCreatedAt(),
                quote.getUpdatedAt(),
                quote.getCategory().getId(),
                quote.getCategory().getName(),
                quote.getTitle(),
                quote.getDescription(),
                quote.getPrice(),
                quote.getCity().getId(),
                quote.getCity().getName(),
                quote.getCity().getDistrict().getName(),
                quote.getCity().getDistrict().getId(),
                quote.getTown(),
                quote.getNearestLandmark(),
                quote.getAddress(),
                coverImage,
                quote.getReviewerApprovalStatus()
        );

        GetQuoteWithMediaResponseData data =
                new GetQuoteWithMediaResponseData(quoteDto, media, features);

        // 7. Cache the assembled DTO data
        advertisementCacheService.putQuoteDetails(
                advertiserId,
                quoteRef,
                data
        );

        return LankaspotResponse.success(
                data,
                "Quote retrieved successfully"
        );
    }

    @Transactional(readOnly = true)
    public LankaspotResponse<GetCurrentAdvertiserContact> getContacts(Authentication authentication) {
        List<ContactDTO> phoneNumbers = contactRepository
                .findByUser_KeycloakUserIdAndStatus(authentication.getName(), ContactStatus.VERIFIED)
                .stream()
                .map(c -> new ContactDTO(c.getPhoneNumber()))
                .toList();

        return LankaspotResponse.success(
                new GetCurrentAdvertiserContact(phoneNumbers),
                "Contacts retrieved successfully");
    }

    @Transactional(readOnly = true)
    public LankaspotResponse<GetResumeQuoteResponseData> getResumeQuote(Authentication authentication) {
        UUID advertiserId = UUID.fromString(authentication.getName());


        Optional<AdQuote> draft = adQuoteRepository.findLatestResumableDraft(advertiserId);


        if (draft.isEmpty()) {
            return LankaspotResponse.success(null, "Nothing to resume");
        }

        AdQuote quote = draft.get();

        List<MediaDefDTO> media = adQuoteMediaRepository
                .findByAdQuote_IdOrderBySortOrderAscIdAsc(quote.getId())
                .stream()
                .map(MediaDefDTO::from)
                .toList();

        List<FeatureDefDTO> features = quote.getFeatures().stream()
                .map(f -> new FeatureDefDTO(
                        f.getFeature().getId(),
                        f.getFeature().getName(),
                        f.getValue(),
                        f.getFeature().getUnit()))
                .sorted(Comparator.comparing(FeatureDefDTO::id))
                .toList();

        String coverImage = media.stream()
                .filter(m -> m.mediaType() == MediaType.IMAGE)
                .filter(m -> m.mediaStatus() != MediaStatus.REQUESTED_DELETION
                        && m.mediaStatus() != MediaStatus.REJECTED)
                .sorted(Comparator.comparing(MediaDefDTO::primary).reversed())
                .map(MediaDefDTO::url)
                .findFirst()
                .orElse(null);

        QuoteDTO quoteDto = new QuoteDTO(
                quote.getFunctionalGroup() != null ? new FunctionalGroupDTO(
                        quote.getFunctionalGroup().getId(),
                        quote.getFunctionalGroup().getName()
                ) : null,
                quote.getId(),
                quote.getId(),          // quoteRef
                quote.getVersion(),
                quote.getSlug(),
                quote.getStatus(),
                quote.getSource(),
                quote.getCreatedAt(),
                quote.getUpdatedAt(),
                quote.getCategory().getId(),
                quote.getCategory().getName(),
                quote.getTitle(),
                quote.getDescription(),
                quote.getPrice(),
                quote.getCity().getId(),
                quote.getCity().getName(),
                quote.getCity().getDistrict().getName(),
                quote.getCity().getDistrict().getId(),
                quote.getTown(),
                quote.getNearestLandmark(),
                quote.getAddress(),
                coverImage,
                quote.getReviewerApprovalStatus()
        );

        return LankaspotResponse.success(
                new GetResumeQuoteResponseData(quoteDto, media, features, quote.getLastCompletedStep()),
                "Resume quote retrieved successfully");
    }

    @Transactional
    public LankaspotResponse<GetReviewQuoteByUserResponseData> reviewQuoteByUser(ReviewQuoteByUserRequestDTO request, Authentication authentication) {
        try{
            log.info("reviewQuoteByUser started: quoteRef={}", request.getQuoteRef());
            UUID advertiserId = UUID.fromString(authentication.getName());

            AdQuote quote = adQuoteRepository.findById(request.getQuoteRef())
                    .filter(q -> Objects.equals(q.getAdvertiserId(), advertiserId))
                    .orElseThrow(() -> new QuoteNotFoundException(request.getQuoteRef()));

            //update last completed step to 3, since user is reviewing the quote
            quote.completeStep(3);

            log.info("Updated last completed step to 3 for quoteRef={}", quote.getId());
            return LankaspotResponse.success(
                    new GetReviewQuoteByUserResponseData(quote.getId()),
                    "Review quote retrieved successfully");

        } catch (Exception e) {
            log.error("Failed to get review quote by user: quoteRef={}", request.getQuoteRef(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public LankaspotResponse<GetSubscriptionPlansResponseData> getSubscriptionPlans() {
        List<SubscriptionPlanDTO> plans = subscriptionPlanRepository
                .findByStatusOrderByPriceAsc(SubscriptionPlanStatus.ACTIVE)
                .stream()
                .map(SubscriptionPlanDTO::from)
                .toList();

        return LankaspotResponse.success(
                new GetSubscriptionPlansResponseData(plans),
                "Subscription plans retrieved successfully");
    }

    @Transactional(readOnly = true)
    public LankaspotResponse<GetAdvertisementTiersResponseData> getAdvertisementTiers() {
        List<AdvertisementTierDTO> tiers = advertisementTierRepository
                .findByActiveTrueOrderByPriceAsc()
                .stream()
                .map(AdvertisementTierDTO::from)
                .toList();

        return LankaspotResponse.success(
                new GetAdvertisementTiersResponseData(tiers),
                "Advertisement tiers retrieved successfully");
    }

    @Transactional
    public LankaspotResponse<SelectAdvertPlanResponseData> selectPlan(SelectAdvertPlanRequest request, Authentication authentication) {
        UUID advertiserId = UUID.fromString(authentication.getName());
        Long quoteRef = request.quoteRef();

        log.info("selectPlan started: quoteRef={}, planId={}, tierCount={}",
                quoteRef, request.planId(), request.tierIds().size());

        // Row lock serialises concurrent saves of the same quote.
        AdQuote quote = adQuoteRepository.findByIdForUpdate(quoteRef)
                .filter(q -> Objects.equals(q.getAdvertiserId(), advertiserId))
                .orElseThrow(() -> new QuoteNotFoundException(quoteRef));

        if (!EDITABLE_STATUSES.contains(quote.getStatus())) {
            throw new QuoteNotEditableException(quoteRef, String.valueOf(quote.getStatus()));
        }

        SubscriptionPlan plan = subscriptionPlanRepository
                .findByIdAndStatus(request.planId(), SubscriptionPlanStatus.ACTIVE)
                .orElseThrow(() -> new PlanNotAvailableException(request.planId()));

        List<AdvertisementTier> tiers = resolveTiers(request.tierIds());

        // One plan per quote: update the existing row, or create it the first time.
        AdQuoteSubscriptionPlan selection = adQuoteSubscriptionPlanRepository
                .findByAdQuote_Id(quoteRef)
                .orElseGet(() -> {
                    AdQuoteSubscriptionPlan created = new AdQuoteSubscriptionPlan();
                    created.setAdQuote(quote);
                    return created;
                });
        selection.setSubscriptionPlan(plan);
        adQuoteSubscriptionPlanRepository.save(selection);

        // Add-ons: remove the ones no longer wanted, insert only the new ones.
        List<AdQuoteTier> current = adQuoteTierRepository.findByAdQuote_Id(quoteRef);
        Set<Long> wantedIds = tiers.stream().map(AdvertisementTier::getId).collect(Collectors.toSet());

        List<AdQuoteTier> stale = current.stream()
                .filter(row -> !wantedIds.contains(row.getAdvertisementTier().getId()))
                .toList();
        adQuoteTierRepository.deleteAll(stale);

        Set<Long> existingIds = current.stream()
                .map(row -> row.getAdvertisementTier().getId())
                .collect(Collectors.toSet());

        List<AdQuoteTier> added = tiers.stream()
                .filter(tier -> !existingIds.contains(tier.getId()))
                .map(tier -> {
                    AdQuoteTier row = new AdQuoteTier();
                    row.setAdQuote(quote);
                    row.setAdvertisementTier(tier);
                    return row;
                })
                .toList();
        adQuoteTierRepository.saveAll(added);

        adQuoteRepository.updateLastCompletedStep(quoteRef, 4);

        log.info("Plan selection saved: quoteRef={}, planId={}, tiersAdded={}, tiersRemoved={}",
                quoteRef, plan.getId(), added.size(), stale.size());
        quote.completeStep(4);
        log.info("Updated last completed step to 4 for quoteRef={}", quote.getId());

        return LankaspotResponse.success(
                new SelectAdvertPlanResponseData(
                        quoteRef,
                        plan.getId(),
                        tiers.stream().map(AdvertisementTier::getId).sorted().toList()),
                "Plan selected successfully");
    }

    @Transactional(readOnly = true)
    public LankaspotResponse<GetAdvertQuotePlanSelectionResponseData> getQuotePlanSelection(Long quoteRef, Authentication authentication) {
        UUID advertiserId = UUID.fromString(authentication.getName());

        adQuoteRepository.findById(quoteRef)
                .filter(q -> Objects.equals(q.getAdvertiserId(), advertiserId))
                .orElseThrow(() -> new QuoteNotFoundException(quoteRef));

        Long planId = adQuoteSubscriptionPlanRepository.findByAdQuote_Id(quoteRef)
                .map(selection -> selection.getSubscriptionPlan().getId())
                .orElse(null);

        List<Long> tierIds = adQuoteTierRepository.findByAdQuote_Id(quoteRef).stream()
                .map(row -> row.getAdvertisementTier().getId())
                .sorted()
                .toList();

        return LankaspotResponse.success(
                new GetAdvertQuotePlanSelectionResponseData(quoteRef, planId, tierIds),
                "Plan selection retrieved successfully");
    }

    @Transactional(readOnly = true)
    public LankaspotResponse<ValidateDiscountCodeResponseData> validateDiscountCode(ValidateDiscountCodeRequest request, Authentication authentication) {
        DiscountCode discount = findApplicableDiscount(request.code(), authentication.getName());

        log.info("Discount code validated: discountCodeId={}, user={}", discount.getId(), authentication.getName());

        return LankaspotResponse.success(
                new ValidateDiscountCodeResponseData(discount.getCode(), discount.getDiscountPercentage()),
                "Discount code applied successfully");
    }

    @Transactional(readOnly = true)
    public LankaspotResponse<Boolean> checkPaymentStatus(String orderId, Authentication authentication) {
        try {
            boolean isSuccess =  paymentRegistryRepository.existsByOrderIdAndStatus(orderId, PaymentRegistryStatus.SUCCESS);
            UUID userId = UUID.fromString(authentication.getName());
            if(isSuccess) {
                notificationProducer.publish(
                        new NotificationEventBody(createNotificationFromOrder(orderId,authentication), userId)
                );
            }
            return LankaspotResponse.success(
                    isSuccess,
                    "Payment status checked successfully"
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Transactional(readOnly = true)
    public LankaspotResponse<GetAllAdvertisementSummaryResponse> getAllAdvertisements(Authentication authentication, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }

        if (size < 1) {
            throw new IllegalArgumentException("Size must be greater than zero");
        }

        UUID advertiserRef = UUID.fromString(authentication.getName());

        // 1. Check Redis cache
        GetAllAdvertisementSummaryResponse cachedResponse =
                advertisementCacheService.getAdvertisementList(
                        advertiserRef,
                        page,
                        size,
                        new TypeReference<GetAllAdvertisementSummaryResponse>() {}
                );

        if (cachedResponse != null) {
            return LankaspotResponse.success(
                    cachedResponse,
                    "Advertisements retrieved successfully"
            );
        }

        // 2. Retrieve advertiser from database
        User advertiser = userRepository
                .findByKeycloakUserId(advertiserRef.toString())
                .orElseThrow(() ->
                        new UserNotFoundException("Advertiser not found"));

        // 3. Retrieve paginated advertisements
        Page<Advertisement> advertisements = advertisementRepository
                .findByAdvertiser_Id(
                        advertiser.getId(),
                        PageRequest.of(page, size)
                );

        // 4. Build advertisement summaries
        List<AdvertisementSummaryDTO> advertisementSummaryDTOs =
                advertisements.getContent()
                        .stream()
                        .map(advertisement -> {
                            Long quoteId = advertisementService
                                    .findQuoteIdByAdvertisementRef(
                                            advertisement.getAdvertisementRef().toString()
                                    );

                            QuoteDTO quote = advertisementService
                                    .getQuoteByQuoteId(quoteId);

                            List<MediaDefDTO> media = advertisementService
                                    .getAllMediaByQuoteId(quoteId);

                            MediaSummaryDTO mediaSummary = media.stream()
                                    .filter(MediaDefDTO::primary)
                                    .findFirst()
                                    .map(item -> new MediaSummaryDTO(
                                            item.url(),
                                            true
                                    ))
                                    .orElseGet(() -> media.stream()
                                            .findFirst()
                                            .map(item -> new MediaSummaryDTO(
                                                    item.url(),
                                                    false
                                            ))
                                            .orElse(null));

                            QuoteSummaryDTO quoteSummary = new QuoteSummaryDTO(
                                    quoteId.toString(),
                                    quote.title(),
                                    quote.city(),
                                    quote.district(),
                                    quote.categoryName(),
                                    quote.price() == null
                                            ? null
                                            : quote.price(),
                                    quote.slug()
                            );

                            boolean propertyAmendmentRequired =
                                    quote.reviewerApprovalStatus()
                                            == ReviewerApprovalStatus.REJECTED;

                            boolean mediaAmendmentRequired = media.stream()
                                    .anyMatch(m ->
                                            m.reviewerApprovalStatus()
                                                    == ReviewerApprovalStatus.REJECTED
                                    );

                            return new AdvertisementSummaryDTO(
                                    propertyAmendmentRequired,
                                    mediaAmendmentRequired,
                                    advertisement.getStatus(),
                                    advertisement.getAdvertisementRef().toString(),
                                    quoteSummary,
                                    mediaSummary
                            );
                        })
                        .toList();

        // 5. Build response with pagination metadata
        GetAllAdvertisementSummaryResponse response =
                new GetAllAdvertisementSummaryResponse(
                        advertisementSummaryDTOs,
                        (long) advertisements.getNumber(),
                        (long) advertisements.getSize(),
                        (long) advertisements.getTotalPages(),
                        advertisements.getTotalElements()
                );

        // 6. Cache the complete response DTO
        advertisementCacheService.putAdvertisementList(
                advertiserRef,
                page,
                size,
                response
        );

        return LankaspotResponse.success(
                response,
                "Advertisements retrieved successfully"
        );
    }

    @Transactional(readOnly = true)
    public LankaspotResponse<GetAdvertisementDTOWithReviewerComment> getAdvertisementByRef(String advertisementRef, Authentication authentication) {
        try {
             //check exist
            User advertiser = userRepository.findByKeycloakUserId(authentication.getName())
                    .orElseThrow(() -> new UserNotFoundException("Advertiser not found"));
            Advertisement advertisement = advertisementRepository.findByAdvertisementRef(UUID.fromString(advertisementRef))
                    .orElseThrow(() -> new AdvertisementNotFoundException(advertisementRef));
            boolean exists = advertisementRepository.existsByAdvertisementRefAndAdvertiser_Id(UUID.fromString(advertisementRef), advertiser.getId());
            if (!exists) {
                throw new AdvertisementNotFoundException(advertisementRef);
            } else {
                AdvertisementDTO advertisementDTO = advertisementService.getAdvertisementByRef(advertisementRef, authentication.getName());
                //get the reason for rejection if the advertisement is rejected
                AdvertisementApproval approval = advertisementApprovalRepository.findFirstByAdvertisement_IdOrderByDecisionAtDesc(advertisement.getId())
                        .orElseThrow(() -> new AdvertisementNotFoundException(advertisementRef));
                GetAdvertisementDTOWithReviewerComment response = new GetAdvertisementDTOWithReviewerComment(advertisementDTO, approval.getReason());

                if (advertisementDTO.status() == AdvertisementStatus.REJECTED) {

                    return LankaspotResponse.success(response, "Advertisement retrieved successfully");
                }

                return LankaspotResponse.success(response, "Advertisement retrieved successfully");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public LankaspotResponse<SaveAdvertPropertyInfoResponseData> amendPropertyInfo(SavePropertyInfoRequest request, Authentication authentication) {
        try {
            UUID advertiserId = UUID.fromString(authentication.getName());

            log.info("savePropertyInfo started: advertiserId={}, quoteRef={}, categoryId={}, cityId={}, featureCount={}",
                    advertiserId, request.getQuoteRef(), request.getCategoryId(), request.getCityId(),
                    request.getFeatures() == null ? 0 : request.getFeatures().size());


            // Update: load the quote first, so ownership/state problems fail fast.
            AdQuote existing = request.getQuoteRef() == null
                    ? null
                    : loadAmendableQuote(request.getQuoteRef(), advertiserId);

            Category category = categoryRepository.findById(request.getCategoryId())
                    .filter(Category::isActive)
                    .orElseThrow(() -> new CategoryNotAvailableException(request.getCategoryId()));

            log.info("Category resolved: id={}, name={}", category.getId(), category.getName());

            City city = cityRepository.findById(request.getCityId())
                    .filter(c -> c.isActive() && c.getDistrict().isActive())
                    .orElseThrow(() -> new CityNotAvailableException(request.getCityId()));

            log.info("City resolved: id={}", city.getId());

            List<ResolvedFeature> features = resolveFeatures(category, request.getFeatures());

            log.info("Features resolved: count={}", features.size());

            Long quoteId = amendUpdateQuote(existing, request, category, city, features);

            // audit log
            auditLogService.record(
                    userRepository.findByKeycloakUserId(authentication.getName())
                            .orElseThrow(() -> new UserNotFoundException("Advertiser not found")),
                    "AMEND_PROPERTY_INFO",
                    "AdQuote",
                    quoteId,
                    existing,
                    request
            );
            //kafka for cache invalidation
            CacheInvalidationBody body = new CacheInvalidationBody(
                    CacheInvalidationEventType.AD_QUOTE_UPDATED,
                    quoteId,
                    advertiserId,
                    existing.getCacheVersion()
            );

            publishCacheInvalidationToKafka(body);

            return LankaspotResponse.success(
                    new SaveAdvertPropertyInfoResponseData(quoteId),
                    "Property info saved successfully");

        } catch (Exception e) {
            log.error("Failed to save property info: quoteRef={}", request.getQuoteRef(), e);
            throw e;
        }
    }

    @Transactional
    public LankaspotResponse<UploadAdvertMediaResponseData> amendUploadMedia(Long quoteRef, Integer primaryIndex, List<MultipartFile> photos, Authentication authentication) {
        log.info(
                "Starting media amendment upload. quoteRef={}, primaryIndex={}, photoCount={}",
                quoteRef,
                primaryIndex,
                photos == null ? 0 : photos.size()
        );

        UUID advertiserId = UUID.fromString(authentication.getName());

        log.info(
                "Authenticated advertiser for media amendment. quoteRef={}, advertiserId={}",
                quoteRef,
                advertiserId
        );

        if (photos == null || photos.isEmpty()) {
            log.warn("Media amendment rejected: no photos provided. quoteRef={}", quoteRef);
            throw new IllegalArgumentException("At least one photo is required");
        }

        if (primaryIndex == null || primaryIndex < 0 || primaryIndex >= photos.size()) {
            log.warn(
                    "Media amendment rejected: invalid primaryIndex. quoteRef={}, primaryIndex={}, photoCount={}",
                    quoteRef,
                    primaryIndex,
                    photos.size()
            );
            throw new IllegalArgumentException("Invalid primary photo index");
        }

        log.info("Fetching and locking quote for amendment. quoteRef={}", quoteRef);

        AdQuote quote = adQuoteRepository.findByIdForUpdate(quoteRef)
                .filter(q -> Objects.equals(q.getAdvertiserId(), advertiserId))
                .orElseThrow(() -> {
                    log.warn(
                            "Media amendment failed: quote not found or advertiser does not own quote. quoteRef={}, advertiserId={}",
                            quoteRef,
                            advertiserId
                    );
                    return new QuoteNotFoundException(quoteRef);
                });

        log.info(
                "Quote retrieved for media amendment. quoteRef={}, status={}",
                quoteRef,
                quote.getStatus()
        );

        if (!AMENDABLE_STATUSES.contains(quote.getStatus())) {
            log.warn(
                    "Media amendment rejected: quote status is not amendable. quoteRef={}, status={}",
                    quoteRef,
                    quote.getStatus()
            );

            throw new QuoteNotAmendableException(
                    quoteRef,
                    String.valueOf(quote.getStatus())
            );
        }

        log.info(
                "Fetching existing non-rejected reviewer media. quoteRef={}",
                quoteRef
        );
        //all media
        List<AdQuoteMedia> allMedia =
                adQuoteMediaRepository.findByAdQuote_IdOrderBySortOrderAscIdAsc(quoteRef);

        //find all media that is not rejected
        List<AdQuoteMedia> existingMedia = allMedia.stream().filter(media -> media.getReviewerApprovalStatus() != ReviewerApprovalStatus.REJECTED).toList();

        log.info(
                "Existing media retrieved. quoteRef={}, existingMediaCount={}",
                quoteRef,
                existingMedia.size()
        );

        // If any matching media does not have a REJECTED media status, reject the amendment.
        boolean hasRejectedMedia = allMedia.stream()
                .anyMatch(media -> media.getReviewerApprovalStatus() == ReviewerApprovalStatus.REJECTED);

        if (!hasRejectedMedia) {
            log.warn(
                    "Media amendment rejected: all existing media has not been rejected. quoteRef={}, existingMediaCount={}",
                    quoteRef,
                    existingMedia.size()
            );

            throw new ReviewerAlreadyAcceptedThisAmendStepException(
                    quoteRef,
                    ApprovalStatus.APPROVED.name()
            );
        }

        final int maxPhotos = 6;

        if (existingMedia.size() + photos.size() > maxPhotos) {
            log.warn(
                    "Media amendment rejected: maximum photo limit exceeded. quoteRef={}, existingMediaCount={}, requestedPhotoCount={}, maxPhotos={}",
                    quoteRef,
                    existingMedia.size(),
                    photos.size(),
                    maxPhotos
            );

            throw new IllegalArgumentException(
                    "A maximum of " + maxPhotos + " photos is allowed per quote"
            );
        }

        log.info(
                "Uploading amendment photos to asset storage. quoteRef={}, photoCount={}",
                quoteRef,
                photos.size()
        );

        List<UploadedAsset> uploadedAssets;

        try {
            uploadedAssets = assetUploaderService.uploadAssets(
                    advertiserId,
                    quoteRef,
                    MediaType.IMAGE,
                    photos
            );

            log.info(
                    "Amendment photos uploaded successfully. quoteRef={}, uploadedAssetCount={}",
                    quoteRef,
                    uploadedAssets.size()
            );
        } catch (IOException exception) {
            log.error(
                    "Failed to upload amendment photos. quoteRef={}, requestedPhotoCount={}",
                    quoteRef,
                    photos.size(),
                    exception
            );

            throw new IllegalStateException(
                    "Unable to upload photos. Please try again.",
                    exception
            );
        }

        try {
            log.info(
                    "Preparing media records for persistence. quoteRef={}, uploadedAssetCount={}, primaryIndex={}",
                    quoteRef,
                    uploadedAssets.size(),
                    primaryIndex
            );

            // The newly selected cover replaces the old cover.
            existingMedia.forEach(media -> media.setPrimary(false));

            int nextSortOrder = existingMedia.stream()
                    .map(AdQuoteMedia::getSortOrder)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(-1) + 1;

            List<AdQuoteMedia> newMedia = new ArrayList<>();

            for (int i = 0; i < uploadedAssets.size(); i++) {
                UploadedAsset asset = uploadedAssets.get(i);

                AdQuoteMedia media = new AdQuoteMedia();
                media.setAdQuote(quote);
                media.setMediaType(asset.mediaType());
                media.setMediaStatus(MediaStatus.PENDING_REVIEW);
                media.setUrl(asset.secureUrl());
                media.setVersion(media.nextAvailableVersion());
                media.setPublicId(asset.publicId());
                media.setSortOrder(nextSortOrder + i);
                media.setPrimary(i == primaryIndex);

                newMedia.add(media);
            }

            log.info(
                    "Saving amendment media records. quoteRef={}, newMediaCount={}, nextSortOrder={}, selectedPrimaryIndex={}",
                    quoteRef,
                    newMedia.size(),
                    nextSortOrder,
                    primaryIndex
            );

            adQuoteMediaRepository.saveAll(newMedia);
            adQuoteMediaRepository.flush();

            log.info(
                    "Amendment media records persisted successfully. quoteRef={}, savedMediaCount={}",
                    quoteRef,
                    newMedia.size()
            );

            List<UploadedAdvertMediaDTO> responseMedia =
                    adQuoteMediaRepository
                            .findByAdQuote_IdOrderBySortOrderAscIdAsc(quoteRef)
                            .stream()
                            .map(UploadedAdvertMediaDTO::from)
                            .toList();

            log.info(
                    "Retrieved updated media for response. quoteRef={}, totalMediaCount={}",
                    quoteRef,
                    responseMedia.size()
            );

            log.info("Recording media amendment audit log. quoteRef={}", quoteRef);

            auditLogService.record(
                    userRepository.findByKeycloakUserId(authentication.getName())
                            .orElseThrow(() -> {
                                log.warn(
                                        "Cannot record media amendment audit log: advertiser user not found. quoteRef={}, advertiserId={}",
                                        quoteRef,
                                        advertiserId
                                );
                                return new UserNotFoundException("Advertiser not found");
                            }),
                    "AMEND_UPLOAD_MEDIA",
                    "AdQuoteMedia",
                    quoteRef,
                    existingMedia,
                    newMedia
            );

            log.info(
                    "Media amendment audit log recorded successfully. quoteRef={}",
                    quoteRef
            );

            log.info(
                    "Media amendment upload completed successfully. quoteRef={}, advertiserId={}, uploadedAssetCount={}, totalMediaCount={}",
                    quoteRef,
                    advertiserId,
                    uploadedAssets.size(),
                    responseMedia.size()
            );

            //kafka for cache invalidation
            CacheInvalidationBody body = new CacheInvalidationBody(
                    CacheInvalidationEventType.AD_QUOTE_MEDIA_UPDATED,
                    quoteRef,
                    advertiserId,
                    quote.getCacheVersion()
            );

            publishCacheInvalidationToKafka(body);

            return LankaspotResponse.success(
                    new UploadAdvertMediaResponseData(quoteRef, responseMedia),
                    "Photos uploaded successfully"
            );

        } catch (RuntimeException exception) {
            log.error(
                    "Media amendment persistence or post-upload processing failed. Attempting asset cleanup. quoteRef={}, uploadedAssetCount={}",
                    quoteRef,
                    uploadedAssets.size(),
                    exception
            );

            // Database persistence or subsequent processing failed after Cloudinary succeeded.
            // Attempt to clean up this upload batch to avoid orphaned assets.
            for (UploadedAsset asset : uploadedAssets) {
                try {
                    assetUploaderService.deleteUploadedAsset(
                            advertiserId,
                            quoteRef,
                            asset.publicId(),
                            asset.resourceType()
                    );

                    log.info(
                            "Cleaned up uploaded asset after media amendment failure. quoteRef={}, resourceType={}",
                            quoteRef,
                            asset.resourceType()
                    );

                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);

                    log.error(
                            "Failed to clean up uploaded asset after media amendment failure. quoteRef={}, resourceType={}",
                            quoteRef,
                            asset.resourceType(),
                            cleanupException
                    );
                }
            }

            log.error(
                    "Media amendment failed after asset upload. Exception will be propagated. quoteRef={}",
                    quoteRef
            );

            throw exception;
        }
    }

    @Transactional
    public LankaspotResponse<RemoveAdvertMediaResponseData> amendRequestMediaRemoval(Long quoteRef, Long mediaId, Authentication authentication) {
        UUID advertiserId = UUID.fromString(authentication.getName());

        AdQuote quote = adQuoteRepository.findByIdForUpdate(quoteRef)
                .filter(q -> Objects.equals(q.getAdvertiserId(), advertiserId))
                .orElseThrow(() -> new QuoteNotFoundException(quoteRef));

        if (!AMENDABLE_STATUSES.contains(quote.getStatus())) {
            throw new QuoteNotAmendableException(
                    quoteRef, String.valueOf(quote.getStatus())
            );
        } else if(quote.getReviewerApprovalStatus() != ReviewerApprovalStatus.REJECTED) {
            throw new ReviewerAlreadyAcceptedThisAmendStepException(quoteRef, String.valueOf(quote.getReviewerApprovalStatus()));
        }

        AdQuoteMedia media = adQuoteMediaRepository
                .findByIdAndAdQuote_Id(mediaId, quoteRef)
                .orElseThrow(() -> new IllegalArgumentException(
                        "The specified photo does not belong to this quote"
                ));

        if (media.getMediaStatus() != MediaStatus.REQUESTED_DELETION) {
            media.setMediaStatus(MediaStatus.REQUESTED_DELETION);
            media.setVersion(media.nextAvailableVersion());
            adQuoteMediaRepository.save(media);
        }
        //kafka for cache invalidation
        CacheInvalidationBody body = new CacheInvalidationBody(
                CacheInvalidationEventType.AD_QUOTE_MEDIA_UPDATED,
                quoteRef,
                advertiserId,
                quote.getCacheVersion()
        );

        publishCacheInvalidationToKafka(body);

        // Do not delete the Cloudinary asset here.
        // The reviewer workflow decides whether to approve the removal.
        return LankaspotResponse.success(
                new RemoveAdvertMediaResponseData(
                        quoteRef, mediaId, media.getMediaStatus()
                ),
                "Photo removal requested successfully"
        );
    }

    @Transactional
    public LankaspotResponse<SetPrimaryAdvertMediaResponseData> amendSetPrimaryMedia(SetPrimaryAdvertMediaRequest request, Authentication authentication) {
        UUID advertiserId = UUID.fromString(authentication.getName());
        Long quoteRef = request.quoteRef();

        AdQuote quote = adQuoteRepository.findByIdForUpdate(quoteRef)
                .filter(q -> Objects.equals(q.getAdvertiserId(), advertiserId))
                .orElseThrow(() -> new QuoteNotFoundException(quoteRef));

        if (!AMENDABLE_STATUSES.contains(quote.getStatus())) {
            throw new QuoteNotAmendableException(
                    quoteRef, String.valueOf(quote.getStatus())
            );
        } else if(quote.getReviewerApprovalStatus() != ReviewerApprovalStatus.REJECTED) {
            throw new ReviewerAlreadyAcceptedThisAmendStepException(quoteRef, String.valueOf(quote.getReviewerApprovalStatus()));
        }

        List<AdQuoteMedia> mediaList =
                adQuoteMediaRepository.findAllByQuoteIdForUpdate(quoteRef);

        AdQuoteMedia selected = mediaList.stream()
                .filter(media -> Objects.equals(
                        media.getId(), request.mediaId()
                ))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "The specified photo does not belong to this quote"
                ));

        if (selected.getMediaStatus() == MediaStatus.REQUESTED_DELETION
                || selected.getMediaStatus() == MediaStatus.REJECTED) {
            throw new IllegalArgumentException(
                    "This photo cannot be selected as the cover"
            );
        }
        //first set all media to primary false, then set the selected media to primary true
        for (AdQuoteMedia media : mediaList) {
            media.setVersion(media.nextAvailableVersion());
            media.setPrimary(false);
        }

        for (AdQuoteMedia media : mediaList) {
            media.setPrimary(Objects.equals(media.getId(), selected.getId()));
            media.setVersion(media.nextAvailableVersion());
        }

        adQuoteMediaRepository.saveAll(mediaList);
        adQuoteMediaRepository.flush();

        //kafka for cache invalidation
        CacheInvalidationBody body = new CacheInvalidationBody(
                CacheInvalidationEventType.AD_QUOTE_MEDIA_UPDATED,
                quoteRef,
                advertiserId,
                quote.getCacheVersion()
        );

        publishCacheInvalidationToKafka(body);

        return LankaspotResponse.success(
                new SetPrimaryAdvertMediaResponseData(
                        quoteRef, selected.getId(), true
                ),
                "Cover photo updated successfully"
        );
    }

    @Transactional
    public LankaspotResponse<GetReviewQuoteByUserResponseData> amendReviewQuoteByUser(ReviewQuoteByUserRequestDTO request, Authentication authentication) {
        try{
            log.info("amendReviewQuoteByUser started: quoteRef={}", request.getQuoteRef());
            UUID advertiserId = UUID.fromString(authentication.getName());

            AdQuote quote = adQuoteRepository.findById(request.getQuoteRef())
                    .filter(q -> Objects.equals(q.getAdvertiserId(), advertiserId))
                    .orElseThrow(() -> new QuoteNotFoundException(request.getQuoteRef()));

            //logic related to completing amendment of quote & policy.
            completeAmendment(quote, authentication);

            //kafka for cache invalidation
            CacheInvalidationBody body = new CacheInvalidationBody(
                    CacheInvalidationEventType.AD_RESUBMITTED_FOR_REVIEW,
                    quote.getId(),
                    quote.getAdvertiserId(),
                    quote.getCacheVersion()
            );

            publishCacheInvalidationToKafka(body);

            return LankaspotResponse.success(
                    new GetReviewQuoteByUserResponseData(quote.getId()),
                    "Review quote retrieved successfully");

        } catch (Exception e) {
            log.error("Failed to get review quote by user: quoteRef={}", request.getQuoteRef(), e);
            throw e;
        }
    }

    @Transactional()
    public LankaspotResponse<?> amendSetAllMediaPrimaryFalse(Long quoteRef, Authentication authentication) {
        try{
            // get all media from quoteRef
            log.info("amendSetAllMediaPrimaryFalse started: quoteRef={}", quoteRef);
            UUID advertiserId = UUID.fromString(authentication.getName());
            AdQuote quote = adQuoteRepository.findByIdForUpdate(quoteRef)
                    .filter(q -> Objects.equals(q.getAdvertiserId(), advertiserId))
                    .orElseThrow(() -> new QuoteNotFoundException(quoteRef));

            if (!AMENDABLE_STATUSES.contains(quote.getStatus())) {
                throw new QuoteNotAmendableException(
                        quoteRef, String.valueOf(quote.getStatus())
                );
            }
            List<AdQuoteMedia> mediaList =
                    adQuoteMediaRepository.findAllByQuoteIdForUpdate(quoteRef);

            // for each set primary to false
            for (AdQuoteMedia media : mediaList) {
                media.setPrimary(false);
            }

            return LankaspotResponse.success(
                    null,
                    "Set all media primary false successfully"
            );



        } catch (Exception e) {
            throw e;
        }
    }


    /**
     * Private functions
     *
     */

    public void publishCacheInvalidationToKafka(CacheInvalidationBody body){
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        cacheInvalidationProducer.publish(body);
                    }
                }
        );
    }

    private void completeAmendment(AdQuote quote, Authentication authentication){
        try{
            //update quote status to AMENDED
            quote.setStatus(AdQuoteStatus.AMENDED);
            adQuoteRepository.save(quote);

            User advertiser = userRepository.findByKeycloakUserId(authentication.getName())
                    .orElseThrow(() -> new UserNotFoundException("Advertiser not found"));

            // create an audit log
            auditLogService.record(advertiser, "AMENDMENT_COMPLETED", "AD_QUOTE", quote.getId(), null, quote);

            //get the advertisement associated with the quote
            Advertisement advertisement = advertisementRepository.findByAdvertisementRef(quote.getAdvertisementRef())
                    .orElseThrow(() -> new AdvertisementNotFoundException(quote.getAdvertisementRef().toString()));
            advertisement.setStatus(AdvertisementStatus.PENDING_REVIEW);
            advertisementRepository.save(advertisement);

            // enqueue the advertisement for the resubmit queue
            reviewerQueueService.enqueueAdvertisementFromResubmitQueue(advertisement);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    private DiscountCode findApplicableDiscount(String code, String keycloakUserId) {
        DiscountCode discount = discountCodeRepository.findByCode(code)
                // Exact comparison on purpose: the database collation may be case-insensitive.
                .filter(d -> d.getCode().equals(code))
                .orElseThrow(() -> new DiscountCodeNotValidException(DISCOUNT_INVALID_MESSAGE));

        LocalDateTime now = LocalDateTime.now();

        boolean usable = discount.isActive()
                && !discount.getValidFrom().isAfter(now)
                && (discount.getValidUntil() == null || discount.getValidUntil().isAfter(now))
                && (discount.getMaxUses() == null || discount.getUsedCount() < discount.getMaxUses());

        if (!usable) {
            throw new DiscountCodeNotValidException(DISCOUNT_INVALID_MESSAGE);
        }

        if (discountCodeUsageRepository.existsByDiscountCode_IdAndUser_KeycloakUserId(discount.getId(), keycloakUserId)) {
            throw new DiscountCodeNotValidException(DISCOUNT_ALREADY_USED_MESSAGE);
        }

        return discount;
    }

    /* ---------------------------------------------------------------------- */
    /* Create / update                                                        */
    /* ---------------------------------------------------------------------- */

    private List<AdvertisementTier> resolveTiers(List<Long> tierIds) {
        if (tierIds.isEmpty()) {
            return List.of();
        }

        Map<Long, AdvertisementTier> found = advertisementTierRepository
                .findByIdInAndActiveTrue(tierIds).stream()
                .collect(Collectors.toMap(AdvertisementTier::getId, t -> t));

        for (Long id : tierIds) {
            if (!found.containsKey(id)) {
                throw new TierNotAvailableException(id);
            }
        }

        return tierIds.stream().map(found::get).toList();
    }

    private Long createQuote(SavePropertyInfoRequest request, UUID advertiserId, Category category, City city, List<ResolvedFeature> features) {
        UUID advertisementRef = UUID.randomUUID();
        String title = request.getTitle().trim();

        AdQuote quote = new AdQuote();
        quote.setAdvertisementRef(advertisementRef);
        quote.setVersion(1);
        quote.setAdvertiserId(advertiserId);
        quote.setCategory(category);
        quote.setTitle(title);
        quote.setFunctionalGroup(request.getFunctionalGroup() != null ? functionalGroupRepository.findById(request.getFunctionalGroup().id()).orElse(null) : null);
        quote.setSlug(createSlug(title, advertisementRef));
        quote.setDescription(blankToNull(request.getDescription()));
        quote.setPrice(request.getPrice());
        quote.setCity(city); // the district is derived from the city
        quote.setTown(request.getTown().trim());
        quote.completeStep(1);
        quote.setNearestLandmark(request.getNearestLandmark().trim());
        quote.setAddress(blankToNull(request.getAddress()));
        quote.setStatus(AdQuoteStatus.IN_COMPLETE);
        quote.setSource(AdSource.USER);
        features.forEach(f -> quote.getFeatures().add(new AdQuoteFeature(quote, f.feature(), f.value())));

        AdQuote saved = adQuoteRepository.save(quote); // cascades the AdQuoteFeature rows

        log.info("Created property info: quoteId={}, ref={}, version={}, advertiserId={}",
                saved.getId(), saved.getAdvertisementRef(), saved.getVersion(), advertiserId);
        return saved.getId();
    }

    /**
     * Updates the quote in place with explicit SQL (AdQuote's columns are not updatable through the
     * entity), increments the version, and replaces its selected features.
     */
    private Long updateQuote(AdQuote existing, SavePropertyInfoRequest request, Category category, City city, List<ResolvedFeature> features) {
        Long quoteId = existing.getId();
        int expectedVersion = existing.getVersion();
        String title = request.getTitle().trim();

        int updated = adQuoteUpdateRepository.updatePropertyInfo(
                quoteId,
                expectedVersion,
                EDITABLE_STATUSES.stream().map(Enum::name).toList(),
                category.getId(),
                title,
                createSlug(title, existing.getAdvertisementRef()),
                blankToNull(request.getDescription()),
                request.getPrice(),
                city.getId(),
                request.getTown().trim(),
                request.getNearestLandmark().trim(),
                blankToNull(request.getAddress()));
        if (updated != 1) {
            // Edited elsewhere (version moved on) or purchased since we loaded it.
            throw new QuoteOutdatedException(quoteId);
        }

        adQuoteUpdateRepository.deleteFeatures(quoteId);
        features.forEach(f -> adQuoteUpdateRepository.insertFeature(quoteId, f.feature().getId(), f.value()));

        log.info("Updated property info: quoteId={}, ref={}, newVersion={}, featureCount={}",
                quoteId, existing.getAdvertisementRef(), expectedVersion + 1, features.size());
        return quoteId;
    }

    private Long amendUpdateQuote(AdQuote existing, SavePropertyInfoRequest request, Category category, City city, List<ResolvedFeature> features) {
        Long quoteId = existing.getId();
        int expectedVersion = existing.getVersion();
        String title = request.getTitle().trim();

        int updated = adQuoteUpdateRepository.updatePropertyInfo(
                quoteId,
                expectedVersion,
                AMENDABLE_STATUSES.stream().map(Enum::name).toList(),
                category.getId(),
                title,
                createSlug(title, existing.getAdvertisementRef()),
                blankToNull(request.getDescription()),
                request.getPrice(),
                city.getId(),
                request.getTown().trim(),
                request.getNearestLandmark().trim(),
                blankToNull(request.getAddress()));
        if (updated != 1) {
            // Edited elsewhere (version moved on) or purchased since we loaded it.
            throw new QuoteOutdatedException(quoteId);
        }

        adQuoteUpdateRepository.deleteFeatures(quoteId);
        features.forEach(f -> adQuoteUpdateRepository.insertFeature(quoteId, f.feature().getId(), f.value()));

        log.info("Updated property info: quoteId={}, ref={}, newVersion={}, featureCount={}",
                quoteId, existing.getAdvertisementRef(), expectedVersion + 1, features.size());
        return quoteId;
    }


    /** Loads the quote being updated and checks that it belongs to the caller and is not purchased yet. */
    private AdQuote loadEditableQuote(Long quoteRef, UUID advertiserId) {AdQuote quote = adQuoteRepository.findById(quoteRef)
                .filter(q -> Objects.equals(q.getAdvertiserId(), advertiserId))
                .orElseThrow(() -> new QuoteNotFoundException(quoteRef));

        if (!EDITABLE_STATUSES.contains(quote.getStatus())) {
            throw new QuoteNotEditableException(quoteRef, String.valueOf(quote.getStatus()));
        }

        log.info("Update accepted: quoteRef={}, status={}, currentVersion={}",
                quote.getId(), quote.getStatus(), quote.getVersion());
        return quote;
    }

    private AdQuote loadAmendableQuote(Long quoteRef, UUID advertiserId) {
        AdQuote quote = adQuoteRepository.findById(quoteRef)
                .filter(q -> Objects.equals(q.getAdvertiserId(), advertiserId))
                .orElseThrow(() -> new QuoteNotFoundException(quoteRef));

        if (!AMENDABLE_STATUSES.contains(quote.getStatus())) {
            throw new QuoteNotAmendableException(quoteRef, String.valueOf(quote.getStatus()));
        }
        else if(quote.getReviewerApprovalStatus() != ReviewerApprovalStatus.REJECTED) {
            throw new ReviewerAlreadyAcceptedThisAmendStepException(quoteRef, String.valueOf(quote.getReviewerApprovalStatus()));
        }

        log.info("Amendment accepted: quoteRef={}, status={}, currentVersion={}",
                quote.getId(), quote.getStatus(), quote.getVersion());
        return quote;
    }

    /* ---------------------------------------------------------------------- */
    /* Features                                                               */
    /* ---------------------------------------------------------------------- */

    private record ResolvedFeature(Feature feature, String value) {}

    private List<ResolvedFeature> resolveFeatures(Category category, List<SavePropertyInfoRequest.FeatureSelection> selections) {

        if (selections == null || selections.isEmpty()) {
            log.info("No features selected");
            return List.of();
        }

        Set<Long> ids = new HashSet<>();
        for (SavePropertyInfoRequest.FeatureSelection selection : selections) {
            if (!ids.add(selection.getFeatureId())) {
                throw new DuplicateFeatureException(selection.getFeatureId());
            }
        }

        Map<Long, Feature> found = featureRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Feature::getId, f -> f));
        Set<Long> allowed = category.getFeatures().stream()
                .map(Feature::getId)
                .collect(Collectors.toSet());
        log.info("Feature lookup: requested={}, found={}, allowedForCategory={} (categoryId={})",
                ids.size(), found.size(), allowed.size(), category.getId());

        List<ResolvedFeature> resolved = new ArrayList<>();
        for (SavePropertyInfoRequest.FeatureSelection selection : selections) {
            Feature feature = found.get(selection.getFeatureId());
            if (feature == null || !feature.isActive() || !allowed.contains(feature.getId())) {
                throw new FeatureNotAvailableException(selection.getFeatureId(), category.getId());
            }
            resolved.add(new ResolvedFeature(feature, normalizeValue(feature, selection.getValue())));
        }
        return resolved;
    }

    /** Applies the feature's valueType: NONE stores null, NUMBER a positive number, TEXT non-blank text. */
    private String normalizeValue(Feature feature, String raw) {
        String value = raw == null ? "" : raw.trim();
        FeatureValueType type = feature.getValueType() == null ? FeatureValueType.NONE : feature.getValueType();

        return switch (type) {
            case NONE -> {
                if (!value.isEmpty()) {
                    throw new InvalidFeatureValueException(feature.getName(), "does not take a value");
                }
                yield null;
            }
            case NUMBER -> {
                if (value.isEmpty()) {
                    throw new InvalidFeatureValueException(feature.getName(), "needs a value");
                }
                BigDecimal number;
                try {
                    number = new BigDecimal(value);
                } catch (NumberFormatException e) {
                    throw new InvalidFeatureValueException(feature.getName(), "must be a number");
                }
                if (number.signum() <= 0) {
                    throw new InvalidFeatureValueException(feature.getName(), "must be greater than 0");
                }
                String plain = number.stripTrailingZeros().toPlainString();
                if (plain.length() > FEATURE_VALUE_MAX_LENGTH) {
                    throw new InvalidFeatureValueException(feature.getName(), "is too large");
                }
                yield plain;
            }
            case TEXT -> {
                if (value.isEmpty()) {
                    throw new InvalidFeatureValueException(feature.getName(), "needs a value");
                }
                yield value;
            }
        };
    }

    /* ---------------------------------------------------------------------- */
    /* Helpers                                                                */
    /* ---------------------------------------------------------------------- */

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String createSlug(String title, UUID advertisementRef) {
        String normalized = title.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return (normalized.isEmpty() ? "property" : normalized) + "-" + advertisementRef;
    }

    private Notification createNotificationFromOrder(String orderId, Authentication authentication) {
        Notification notification = new Notification();
        notification.setTitle(NotificationConstants.AD_PURCHASED_SUCCESSFULLY_TITLE);
        notification.setDescription(NotificationConstants.AD_PURCHASED_SUCCESSFULLY_DESCRIPTION);
        notification.setRecipientId(utilService.resolveUUID(authentication));
        notification.setRecipientRole(utilService.resolveRole(authentication));
        notification.setRedirectTo(NotificationConstants.AD_PURCHASED_SUCCESSFULLY_REDIRECT_TO + orderId);
        notification.setType(NotificationType.CREATE_ADVERTISEMENT_PURCHASED_SUCCESSFULLY);

        //get quoteRef from order
        PaymentRegistry paymentRegistry = paymentRegistryRepository.findByOrderId(orderId).orElse(null);
        if(paymentRegistry == null) {
            throw new AdvertisementNotFoundException(orderId);
        }
        AdQuote quote = adQuoteRepository.findById(paymentRegistry.getQuoteRef()).orElse(null);
        if(quote == null) {
            throw new QuoteNotFoundException(paymentRegistry.getQuoteRef());
        }
        String coverUrl = adQuoteMediaRepository
                .findAllByQuoteId(paymentRegistry.getQuoteRef())
                .stream()
                .filter(AdQuoteMedia::isPrimary)
                .map(AdQuoteMedia::getUrl)
                .findFirst()
                .orElse(null);

        notification.setImageUrl(coverUrl);

        return notification;

    }
}