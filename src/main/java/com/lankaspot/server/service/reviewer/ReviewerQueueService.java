package com.lankaspot.server.service.reviewer;

import com.lankaspot.server.dto.advertiser.advertisement.FunctionalGroupDTO;
import com.lankaspot.server.dto.advertiser.retrieve.*;
import com.lankaspot.server.dto.reviewer.retrieve.*;
import com.lankaspot.server.enums.ContactStatus;
import com.lankaspot.server.enums.EntityMappingType;
import com.lankaspot.server.enums.MediaStatus;
import com.lankaspot.server.model.*;
import com.lankaspot.server.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewerQueueService {

    private final AdvertisementReviewQueueRepository advertisementReviewQueueRepository;
    private final AdvertisementRepository advertisementRepository;
    private final AdQuoteRepository adQuoteRepository;
    private final AdQuoteMediaRepository adQuoteMediaRepository;
    private final EntityMappingRepository entityMappinngRepository;
    private final AdPolicyRepository adPolicyRepository;
    private final ContactRepository contactRepository;
    private final AdvertisementReviewResubmitQueueRepository advertisementReviewResubmitQueueRepository;
    private final AdvertisementApprovalRepository advertisementApprovalRepository;

    public void enqueueAdvertisementFromQueue(Advertisement ad) {
        log.info("Enqueuing advertisement. advertisementId={}", ad.getId());

        AdvertisementReviewQueue reviewQueueEntry = new AdvertisementReviewQueue();
        reviewQueueEntry.setAdvertisement(ad);
        reviewQueueEntry.setCreatedAt(LocalDateTime.now());

        advertisementReviewQueueRepository.save(reviewQueueEntry);

        log.info("Advertisement successfully added to review queue. advertisementId={}",
                ad.getId());
    }

    public void dequeueAdvertisementFromQueue(Advertisement ad) {
        log.info("Attempting to remove advertisement from review queue. advertisementId={}",
                ad.getId());

        AdvertisementReviewQueue reviewQueueEntry =
                advertisementReviewQueueRepository.findByAdvertisementId(ad.getId());

        if (reviewQueueEntry != null) {
            advertisementReviewQueueRepository.delete(reviewQueueEntry);

            log.info("Advertisement removed from review queue. advertisementId={}",
                    ad.getId());
        } else {
            log.warn("Advertisement was not found in review queue. advertisementId={}",
                    ad.getId());
        }
    }

    public boolean isAdvertisementInReviewQueue(Advertisement ad) {
        return advertisementReviewQueueRepository.existsByAdvertisementId(ad.getId());
    }
    public boolean isAdvertisementInResubmitQueue(Advertisement ad) {
        return advertisementReviewResubmitQueueRepository.existsByAdvertisementId(ad.getId());
    }

    public ReviewerQueueDTO getAllAdvertisementsInQueue(int page, int size) {
        log.info("Retrieving reviewer queue. page={}, size={}", page, size);

        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }

        if (size < 1) {
            throw new IllegalArgumentException("Size must be greater than zero");
        }

        Page<AdvertisementReviewQueue> queuePage =
                advertisementReviewQueueRepository.findAllByOrderByCreatedAtAscIdAsc(
                        PageRequest.of(page, size)
                );

        List<ReviewerAdvertisementSummary> advertisements =
                queuePage.getContent().stream()
                        .map(entry -> new ReviewerAdvertisementSummary(
                                entry.getAdvertisement()
                                        .getAdvertisementRef()
                                        .toString(),
                                entry.getAdvertisement().getTitle()
                        ))
                        .toList();

        log.info(
                "Reviewer queue retrieved successfully. page={}, size={}, totalElements={}",
                page,
                size,
                queuePage.getTotalElements()
        );

        return new ReviewerQueueDTO(
                advertisements,
                (long) queuePage.getNumber(),
                (long) queuePage.getSize(),
                (long) queuePage.getTotalPages(),
                queuePage.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public GetReviewerFullAdvertisement getAdvertisementForReview(String advertisementRef) {
        log.info("Starting advertisement review retrieval. advertisementRef={}",
                advertisementRef);

        try {
            // 1. Find advertisement
            UUID advertRef = UUID.fromString(advertisementRef);

            Advertisement ad = advertisementRepository.findByAdvertisementRef(advertRef)
                    .orElseThrow(() -> {
                        log.warn("Advertisement not found. advertisementRef={}",
                                advertisementRef);
                        return new IllegalArgumentException("Advertisement not found");
                    });

            log.info("Advertisement retrieved. advertisementId={}", ad.getId());

            // 2. Validate review queue entry
            AdvertisementReviewQueue reviewQueueEntry =
                    advertisementReviewQueueRepository.findByAdvertisementId(ad.getId());
            // check if the advertisement is in the review resubmit queue
            AdvertisementReviewResubmitQueue reviewResubmitQueueEntry =
                    advertisementReviewResubmitQueueRepository.findByAdvertisementId(ad.getId());

            if (reviewQueueEntry == null && reviewResubmitQueueEntry == null) {
                log.warn("Advertisement not found in review or resubmit queue. advertisementId={}",
                        ad.getId());
                throw new IllegalArgumentException(
                        "Advertisement not found in review or resubmit queue"
                );
            }

            log.info("Review or resubmit queue entry validated. advertisementId={}", ad.getId());

            // 3. Retrieve advertisement policy
            AdPolicy policy = adPolicyRepository.findByAdvertisement_Id(ad.getId())
                    .orElseThrow(() -> {
                        log.warn("Policy not found. advertisementId={}", ad.getId());
                        return new IllegalArgumentException(
                                "Policy not found for advertisement"
                        );
                    });

            log.info("Advertisement policy retrieved. policyId={}", policy.getId());

            // 4. Retrieve quote-policy mapping
            EntityMapping entityMapping =
                    entityMappinngRepository.findIdByPolicy_IdAndMappingType(
                                    policy.getId(),
                                    EntityMappingType.QUOTE_POLICY
                            )
                            .orElseThrow(() -> {
                                log.warn("Quote-policy mapping not found. policyId={}",
                                        policy.getId());
                                return new IllegalArgumentException(
                                        "Quote mapping not found for policy"
                                );
                            });

            Long quoteId = entityMapping.getQuote().getId();

            log.info("Quote-policy mapping retrieved. policyId={}, quoteId={}",
                    policy.getId(), quoteId);

            // 5. Retrieve advertisement quote
            AdQuote quote = adQuoteRepository.findById(quoteId)
                    .orElseThrow(() -> {
                        log.warn("Quote not found. quoteId={}", quoteId);
                        return new IllegalArgumentException(
                                "Quote not found for policy"
                        );
                    });

            log.info("Advertisement quote retrieved. quoteId={}, version={}",
                    quote.getId(), quote.getVersion());

            // 6. Retrieve quote media
            List<MediaDefDTO> media = adQuoteMediaRepository
                    .findByAdQuote_IdAndMediaStatusNotOrderBySortOrderAscIdAsc(quote.getId(), MediaStatus.REQUESTED_DELETION)
                    .stream()
                    .map(MediaDefDTO::from)
                    .toList();

            log.info("Quote media retrieved. quoteId={}, mediaCount={}",
                    quote.getId(), media.size());

            // 7. Retrieve quote features
            List<FeatureDefDTO> features = quote.getFeatures().stream()
                    .map(f -> new FeatureDefDTO(
                            f.getFeature().getId(),
                            f.getFeature().getName(),
                            f.getValue(),
                            f.getFeature().getUnit()
                    ))
                    .sorted(Comparator.comparing(FeatureDefDTO::id))
                    .toList();

            log.info("Quote features retrieved. quoteId={}, featureCount={}",
                    quote.getId(), features.size());

            // 8. Determine cover image
            String coverImage = media.stream()
                    .filter(MediaDefDTO::primary)
                    .findFirst()
                    .map(MediaDefDTO::url)
                    .orElse(null);

            log.info("Cover image resolved. quoteId={}, available={}",
                    quote.getId(), coverImage != null);

            // 9. Build quote DTO
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

            log.info("Quote DTO constructed. quoteId={}", quote.getId());

            // 10. Retrieve verified advertiser contacts
            List<Contact> contacts = contactRepository
                    .findByUser_KeycloakUserIdAndStatus(
                            ad.getAdvertiser().getKeycloakUserId().toString(),
                            ContactStatus.VERIFIED
                    );

            log.info("Verified advertiser contacts retrieved. advertisementId={}, count={}",
                    ad.getId(), contacts.size());

            // 11. Build final reviewer response
            GetReviewerFullAdvertisement response =
                    new GetReviewerFullAdvertisement(
                            getReasonForResubmit(ad),
                            advertisementRef,
                            quoteDto,
                            media,
                            features,
                            contacts.stream()
                                    .map(c -> new ContactDTO(c.getPhoneNumber()))
                                    .toList()
                    );

            log.info("Advertisement review retrieval completed. advertisementRef={}",
                    advertisementRef);

            return response;

        } catch (RuntimeException e) {
            log.error("Failed to retrieve advertisement for review. advertisementRef={}",
                    advertisementRef, e);
            throw e;
        }
    }

    public void enqueueAdvertisementFromResubmitQueue(Advertisement ad) {
        log.info("Enqueuing advertisement for resubmit queue. advertisementId={}", ad.getId());

        AdvertisementReviewResubmitQueue reviewResubmitQueueEntry = new AdvertisementReviewResubmitQueue();
        reviewResubmitQueueEntry.setAdvertisement(ad);
        reviewResubmitQueueEntry.setCreatedAt(LocalDateTime.now());

        advertisementReviewResubmitQueueRepository.save(reviewResubmitQueueEntry);

        log.info("Advertisement successfully added to resubmit queue. advertisementId={}",
                ad.getId());
    }

    public void dequeueAdvertisementFromResubmitQueue(Advertisement ad) {
        log.info("Attempting to remove advertisement from resubmit queue. advertisementId={}",
                ad.getId());

        AdvertisementReviewResubmitQueue reviewResubmitQueueEntry =
                advertisementReviewResubmitQueueRepository.findByAdvertisementId(ad.getId());

        if (reviewResubmitQueueEntry != null) {
            advertisementReviewResubmitQueueRepository.delete(reviewResubmitQueueEntry);

            log.info("Advertisement removed from resubmit queue. advertisementId={}",
                    ad.getId());
        } else {
            log.warn("Advertisement was not found in resubmit queue. advertisementId={}",
                    ad.getId());
        }
    }

    public GetReviewerResubmitQueueDTO getAllAdvertisementsInResubmitQueue(int page, int size) {
        log.info("Retrieving reviewer resubmit queue. page={}, size={}", page, size);

        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }

        if (size < 1) {
            throw new IllegalArgumentException("Size must be greater than zero");
        }

        Page<AdvertisementReviewResubmitQueue> queuePage =
                advertisementReviewResubmitQueueRepository.findAllByOrderByCreatedAtAscIdAsc(
                        PageRequest.of(page, size)
                );

        List<ReviewerResubmitAdvertisementSummary> advertisements =
                queuePage.getContent().stream()
                        .map(entry -> new ReviewerResubmitAdvertisementSummary(
                                entry.getAdvertisement()
                                        .getAdvertisementRef()
                                        .toString(),
                                entry.getAdvertisement().getTitle(),
                                getReasonForResubmit(entry.getAdvertisement())
                        ))
                        .toList();

        log.info(
                "Reviewer resubmit queue retrieved successfully. page={}, size={}, totalElements={}",
                page,
                size,
                queuePage.getTotalElements()
        );

        return new GetReviewerResubmitQueueDTO(
                advertisements,
                (long) queuePage.getNumber(),
                (long) queuePage.getSize(),
                (long) queuePage.getTotalPages(),
                queuePage.getTotalElements()
        );
    }

    private String getReasonForResubmit(Advertisement ad) {
        Optional<AdvertisementApproval> latestApproval = advertisementApprovalRepository.findFirstByAdvertisement_IdOrderByDecisionAtDesc(ad.getId());
        return latestApproval.map(AdvertisementApproval::getReason).orElse(null);
    }
}
