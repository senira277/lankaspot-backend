package com.lankaspot.server.service.reviewer;

import com.lankaspot.server.constants.NotificationConstants;
import com.lankaspot.server.event.model.cacheInvalidation.CacheInvalidationBody;
import com.lankaspot.server.dto.reviewer.ReviewConfirmationRequest;
import com.lankaspot.server.dto.reviewer.ReviewerConfirmationResponse;
import com.lankaspot.server.dto.reviewer.retrieve.GetReviewerFullAdvertisement;
import com.lankaspot.server.dto.reviewer.retrieve.GetReviewerResubmitQueueDTO;
import com.lankaspot.server.dto.reviewer.retrieve.ReviewerQueueDTO;
import com.lankaspot.server.enums.*;
import com.lankaspot.server.event.model.notification.NotificationEventBody;
import com.lankaspot.server.exception.PolicyNotFoundException;
import com.lankaspot.server.model.*;
import com.lankaspot.server.repository.*;
import com.lankaspot.server.response.LankaspotResponse;
import com.lankaspot.server.service.AdvertiserService;
import com.lankaspot.server.service.AuditLogService;
import com.lankaspot.server.service.kafka.NotificationProducer;
import com.lankaspot.server.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewerService {
    private final ReviewerQueueService reviewerQueueService;
    private final AdQuoteMediaRepository adQuoteMediaRepository;
    private final AdQuoteRepository adQuoteRepository;
    private final AdPolicyRepository adPolicyRepository;
    private final EntityMappingRepository entityMappingRepository;
    private final ReviewerQueueService queueService;
    private final AdvertisementApprovalRepository advertisementApprovalRepository;
    private final UserRepository userRepository;
    private final AdvertisementRepository advertisementRepository;
    private final AuditLogService auditLogService;
    private final AdvertiserService  advertiserService;
    private final NotificationProducer notificationProducer;
    private final NotificationService notificationService;

    //get review queue for reviewer
    @Transactional(readOnly = true)
    public LankaspotResponse<ReviewerQueueDTO> getReviewerQueue(int page, int size) {
        try{
            // get advertisements
            log.info("Retrieving reviewer queue for page: {}, size: {}", page, size);
            ReviewerQueueDTO reviewerQueueDTO = reviewerQueueService.getAllAdvertisementsInQueue(page, size);
            return LankaspotResponse.success(reviewerQueueDTO);

        } catch (Exception e) {
            log.error("Error occurred while retrieving reviewer queue", e);
        }
        return LankaspotResponse.error("Failed to retrieve reviewer queue");
    }

    @Transactional(readOnly = true)
    public LankaspotResponse<GetReviewerFullAdvertisement> getAdvertisementForReview(String advertisementRef) {
        try {
            log.info("Retrieving advertisement for review with ref: {}", advertisementRef);
            GetReviewerFullAdvertisement advertisement = reviewerQueueService.getAdvertisementForReview(advertisementRef);
            return LankaspotResponse.success(advertisement);
        } catch (Exception e) {
            log.error("Error occurred while retrieving advertisement for review", e);
        }
        return LankaspotResponse.error("Failed to retrieve advertisement for review");
    }

    @Transactional
    public LankaspotResponse<ReviewerConfirmationResponse> reviewerConfirmation(ReviewConfirmationRequest request, Authentication authentication) {
        try {
            log.info("Processing reviewer confirmation for request: {}", request);

            UUID advertisementRef = UUID.fromString(request.advertisementRef());
            Advertisement advertisement = advertisementRepository.findByAdvertisementRef(advertisementRef)
                    .orElseThrow(() -> new RuntimeException("Advertisement not found for ref: " + advertisementRef));


            // identify whether review queue or resubmit queue
            AtomicBoolean isInReviewQueue = new AtomicBoolean(false);
            isInReviewQueue = reviewerQueueService.isAdvertisementInReviewQueue(advertisement) ? new AtomicBoolean(true) : new AtomicBoolean(false);

            //flag to spot if any media is rejected
            AtomicBoolean anyMediaRejected = new AtomicBoolean(false);
            AtomicBoolean quoteRejected = new AtomicBoolean(false);

            // for each media in the request, update media, reviewrApprovalStatus in AdQuoteMediaRepository
            request.media().forEach(mediaConfirmation -> {
                if(mediaConfirmation.reviewerApprovalStatus() == ReviewerApprovalStatus.REJECTED) {
                    adQuoteMediaRepository.updateMediaApprovalStatus(
                            mediaConfirmation.id(),
                            MediaStatus.REQUESTED_DELETION
                    );
                    anyMediaRejected.set(true);

                    // handle scenario where media is rejected but the media is the primary media.
                    // in this case, all media for the quote should be is_primary false
                    AdQuoteMedia rejectedMedia = adQuoteMediaRepository.findById(mediaConfirmation.id())
                            .orElseThrow(() -> new RuntimeException("Media not found for id: " + mediaConfirmation.id()));
                    if(rejectedMedia.isPrimary()) {
//                        List<AdQuoteMedia> mediaList = adQuoteMediaRepository.findByAdQuote_IdOrderBySortOrderAscIdAsc(rejectedMedia.getAdQuote().getId());
//                        Optional<AdQuoteMedia> nextPrimaryMedia = mediaList.stream()
//                                .filter(media -> !media.getId().equals(rejectedMedia.getId()) && media.getMediaStatus() != MediaStatus.REJECTED)
//                                .findFirst();
//                        if(nextPrimaryMedia.isPresent()) {
//                            AdQuoteMedia nextMedia = nextPrimaryMedia.get();
//                            nextMedia.setPrimary(true);
//                            adQuoteMediaRepository.save(nextMedia);
//                        } else {
//                            rejectedMedia.setPrimary(false);
//                            adQuoteMediaRepository.save(rejectedMedia);
//                        }
                        // set all media for the quote to is_primary false
                        List<AdQuoteMedia> mediaList = adQuoteMediaRepository.findByAdQuote_IdOrderBySortOrderAscIdAsc(rejectedMedia.getAdQuote().getId());
                        mediaList.forEach(media -> {
                            media.setPrimary(false);
                            adQuoteMediaRepository.save(media);
                        });
                    }

                } else{
                    adQuoteMediaRepository.updateMediaApprovalStatus(
                            mediaConfirmation.id(),
                            MediaStatus.valueOf(mediaConfirmation.reviewerApprovalStatus().name())
                    );
                }


                adQuoteMediaRepository.updateReviewerApprovalStatus(
                        mediaConfirmation.id(),
                        ReviewerApprovalStatus.valueOf(mediaConfirmation.reviewerApprovalStatus().name())
                );

            });
            // update quote(search advertisementRef for in AdPolicyRepository, then entity mapping-> get quote) status in AdQuoteRepository


            AdPolicy adPolicy = adPolicyRepository.findByAdvertisement_Id(advertisement.getId())
                    .orElseThrow(() -> new PolicyNotFoundException(request.advertisementRef()));

            //now find quote in entity mapping
            Optional<EntityMapping> entityMappingOptional = entityMappingRepository.findIdByPolicy_IdAndMappingType(
                    adPolicy.getId(),
                   EntityMappingType.QUOTE_POLICY
            );
            AdQuote quote = entityMappingOptional.get().getQuote();

            adQuoteRepository.updateQuoteApprovalStatusAndStatus(
                    quote.getId(),
                    ReviewerApprovalStatus.valueOf(request.quote().reviewerApprovalStatus().name()),
                    request.status() == ApprovalStatus.APPROVED ? AdQuoteStatus.COMPLETE : AdQuoteStatus.REQUESTED_CHANGES
            );
            if (request.quote().reviewerApprovalStatus() == ApprovalStatus.REJECTED) {
                quoteRejected.set(true);
            }
            //create the AdvertisementApproval record
            AdvertisementApproval advertisementApproval = new AdvertisementApproval();
            advertisementApproval.setAdvertisement(adPolicy.getAdvertisement());
            UUID advertiserId = UUID.fromString(advertisement.getAdvertiser().getKeycloakUserId());
            //set the approval status based on the request status and media rejection
            if(!quoteRejected.get() && !anyMediaRejected.get()) {
                //Reviewer APPROVED
                //update all required


                //publish notification for the advertiser
                notificationProducer.publish(
                        new NotificationEventBody(notificationService.createNotificationForAdvertiser(
                                advertisement.getAdvertisementRef().toString(),
                                advertiserId,
                                NotificationConstants.AD_APPROVED_TITLE,
                                NotificationConstants.AD_APPROVED_DESCRIPTION,
                                NotificationConstants.AD_APPROVED_REDIRECT_TO,
                                NotificationType.ADVERTISEMENT_APPROVED
                        ), advertiserId)
                );
                advertisementApproval.setStatus(ApprovalStatus.APPROVED);
                advertisementRepository.updateStatusById(advertisement.getId(), AdvertisementStatus.ACTIVE);
            } else {
                //Reviewer REJECTED
                boolean propertyAmendmentRequired = quoteRejected.get();
                boolean mediaAmendmentRequired = anyMediaRejected.get();


                // Build the base URL for the rejected advertisement
                String RejectedBaseUrl = NotificationConstants.AD_REJECTED_REDIRECT_TO + "?advertisementRef=" + advertisement.getAdvertisementRef().toString();

                if(propertyAmendmentRequired && mediaAmendmentRequired) {
                    RejectedBaseUrl += "&property=true&media=true";
                } else if(propertyAmendmentRequired) {
                    RejectedBaseUrl += "&property=true";
                } else if(mediaAmendmentRequired) {
                    RejectedBaseUrl += "&media=true";
                }

                //publish notification for the advertiser
                notificationProducer.publish(
                        new NotificationEventBody(notificationService.createNotificationForAdvertiser(
                                advertisement.getAdvertisementRef().toString(),
                                advertiserId,
                                NotificationConstants.AD_REJECTED_TITLE,
                                NotificationConstants.AD_REJECTED_DESCRIPTION,
                                RejectedBaseUrl,
                                NotificationType.ADVERTISEMENT_REJECTED
                        ), advertiserId)
                );
                advertisementApproval.setStatus(ApprovalStatus.REJECTED);
                advertisementRepository.updateStatusById(advertisement.getId(), AdvertisementStatus.REJECTED);
            }

            //add to Audit log
             User reviewer = userRepository.findByKeycloakUserId(authentication.getName())
                                .orElseThrow(() -> new RuntimeException("Reviewer not found"));

            auditLogService.record(reviewer, "REVIEWER_CONFIRMATION", "AdvertisementApproval", advertisementApproval.getId(), null, advertisementApproval);

            advertisementApproval.setReason(request.reason());
            advertisementApproval.setReviewer(reviewer);
            advertisementApproval.setPolicy(adPolicy);
            advertisementApproval.setCreatedAt(java.time.LocalDateTime.now());
            advertisementApproval.setDecisionAt(java.time.LocalDateTime.now());
            advertisementApprovalRepository.save(advertisementApproval);

            //deque the advertisement from the relevant queue
            if(isInReviewQueue.get()){
                queueService.dequeueAdvertisementFromQueue(advertisement);
            }else {
                queueService.dequeueAdvertisementFromResubmitQueue(advertisement);
            }


            //create response
            ReviewerConfirmationResponse response = new ReviewerConfirmationResponse(
                    advertisement.getAdvertisementRef().toString(),
                    advertisementApproval.getStatus(),
                    advertisementApproval.getReason()
            );
            log.info("Successfully processed advertisement for reviewer: {}", response);

            //kafka for cache invalidation
            CacheInvalidationBody body = new CacheInvalidationBody(
                    CacheInvalidationEventType.AD_REVIEW_COMPLETED,
                    quote.getId(),
                    quote.getAdvertiserId(),
                    quote.getCacheVersion()
            );

            advertiserService.publishCacheInvalidationToKafka(body);


            return LankaspotResponse.success(response);
        } catch (Exception e) {
            log.error("Error occurred while processing reviewer confirmation", e);
        }
        return LankaspotResponse.error("Failed to process reviewer confirmation");
    }

    @Transactional(readOnly = true)
    public LankaspotResponse<GetReviewerResubmitQueueDTO> getReviewerResubmitQueue(int page, int size) {
        try{
            // get advertisements
            log.info("Retrieving reviewer queue for page: {}, size: {}", page, size);
            GetReviewerResubmitQueueDTO reviewerQueueDTO = reviewerQueueService.getAllAdvertisementsInResubmitQueue(page, size);
            return LankaspotResponse.success(reviewerQueueDTO);

        } catch (Exception e) {
            log.error("Error occurred while retrieving reviewer queue", e);
        }
        return LankaspotResponse.error("Failed to retrieve reviewer queue");
    }

}
