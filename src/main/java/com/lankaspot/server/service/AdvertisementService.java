package com.lankaspot.server.service;

import com.lankaspot.server.dto.advertiser.advertisement.AdvertisementDTO;
import com.lankaspot.server.dto.advertiser.advertisement.FunctionalGroupDTO;
import com.lankaspot.server.dto.advertiser.retrieve.ContactDTO;
import com.lankaspot.server.dto.advertiser.retrieve.FeatureDefDTO;
import com.lankaspot.server.dto.advertiser.retrieve.MediaDefDTO;
import com.lankaspot.server.dto.advertiser.retrieve.QuoteDTO;
import com.lankaspot.server.enums.ContactStatus;
import com.lankaspot.server.enums.EntityMappingType;
import com.lankaspot.server.enums.MediaStatus;
import com.lankaspot.server.exception.AdvertisementNotFoundException;
import com.lankaspot.server.model.*;
import com.lankaspot.server.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdvertisementService {
    private final AdvertisementRepository advertisementRepository;
    private final AdQuoteRepository adQuoteRepository;
    private final AdQuoteMediaRepository adQuoteMediaRepository;
    private final EntityMappingRepository entityMappinngRepository;
    private final AdPolicyRepository adPolicyRepository;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;


    public QuoteDTO getQuoteByQuoteId(Long quoteId) {
        AdQuote quote = adQuoteRepository.findById(quoteId)
                .orElseThrow(() -> {
                    log.warn("Quote not found. quoteId={}", quoteId);
                    return new IllegalArgumentException(
                            "Quote not found for the given ID"
                    );
                });
        List<MediaDefDTO> media = adQuoteMediaRepository
                .findByAdQuote_IdOrderBySortOrderAscIdAsc(quote.getId())
                .stream()
                .map(MediaDefDTO::from)
                .toList();

        String coverImage = media.stream()
                .filter(MediaDefDTO::primary)
                .findFirst()
                .map(MediaDefDTO::url)
                .orElse(null);

        return new QuoteDTO(
                quote.getFunctionalGroup() != null ? new FunctionalGroupDTO(
                        quote.getFunctionalGroup().getId(),
                        quote.getFunctionalGroup().getName()
                ) : null,
                quote.getId(),
                quote.getId(),
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
    }

    public List<FeatureDefDTO> getAllFeaturesByQuoteId(Long quoteId) {
        AdQuote quote = adQuoteRepository.findById(quoteId)
                .orElseThrow(() -> {
                    log.warn("Quote not found. quoteId={}", quoteId);
                    return new IllegalArgumentException(
                            "Quote not found for the given ID"
                    );
                });
        return quote.getFeatures().stream()
                .map(f -> new FeatureDefDTO(
                        f.getFeature().getId(),
                        f.getFeature().getName(),
                        f.getValue(),
                        f.getFeature().getUnit()
                ))
                .sorted(Comparator.comparing(FeatureDefDTO::id))
                .toList();

    }

    public List<ContactDTO> getAllContactsByKeycloakUserId(String keycloakUserId) {
        return contactRepository.findByUser_KeycloakUserIdAndStatus(
                keycloakUserId,
                ContactStatus.VERIFIED
        ).stream()
                .map(c -> new ContactDTO(
                        c.getPhoneNumber()
                ))
                .toList();
    }

    public List<MediaDefDTO> getAllMediaByQuoteId(Long quoteId) {
        return adQuoteMediaRepository
                .findByAdQuote_IdAndMediaStatusNotOrderBySortOrderAscIdAsc(quoteId, MediaStatus.REQUESTED_DELETION)
                .stream()
                .map(MediaDefDTO::from)
                .toList();
    }

    public Long findQuoteIdByAdvertisementRef(String advertisementRef) {
        UUID adId = UUID.fromString(advertisementRef);
        Advertisement ad = advertisementRepository.findByAdvertisementRef(adId)
                .orElseThrow(() -> {
                    log.warn("Advertisement not found. advertisementRef={}", advertisementRef);
                    return new IllegalArgumentException(
                            "Advertisement not found for the given reference"
                    );
                });
        AdPolicy policy = adPolicyRepository.findByAdvertisement_Id(ad.getId())
                .orElseThrow(() -> {
                    log.warn("Policy not found. advertisementId={}", ad.getId());
                    return new IllegalArgumentException(
                            "Policy not found for advertisement"
                    );
                });

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
        return entityMapping.getQuote().getId();
    }

    public Long findPolicyIdByAdvertisementRef(String advertisementRef) {
        UUID adId = UUID.fromString(advertisementRef);
        Advertisement ad = advertisementRepository.findByAdvertisementRef(adId)
                .orElseThrow(() -> {
                    log.warn("Advertisement not found. advertisementRef={}", advertisementRef);
                    return new IllegalArgumentException(
                            "Advertisement not found for the given reference"
                    );
                });
        AdPolicy policy = adPolicyRepository.findByAdvertisement_Id(ad.getId())
                .orElseThrow(() -> {
                    log.warn("Policy not found. advertisementId={}", ad.getId());
                    return new IllegalArgumentException(
                            "Policy not found for advertisement"
                    );
                });
        return policy.getId();
    }

    public AdvertisementDTO getAdvertisementByRef(String advertisementRef, String keycloakUserId) {
        Advertisement advertisement = advertisementRepository.findByAdvertisementRef(UUID.fromString(advertisementRef))
                .orElseThrow(() -> new AdvertisementNotFoundException(advertisementRef));

        Long quoteId = findQuoteIdByAdvertisementRef(advertisement.getAdvertisementRef().toString());
        QuoteDTO quoteDTO = getQuoteByQuoteId(quoteId);
        List<MediaDefDTO> mediaDefDTOs = getAllMediaByQuoteId(quoteId);
        List<FeatureDefDTO> featureDefDTOs = getAllFeaturesByQuoteId(quoteId);
        List<ContactDTO> contactDTOs = getAllContactsByKeycloakUserId(keycloakUserId);



        return new AdvertisementDTO(
                advertisement.getStatus(),
                advertisement.getAdvertisementRef().toString(),
                quoteDTO,
                mediaDefDTOs,
                featureDefDTOs,
                contactDTOs
        );
    }

    public String getAdvertiserEmailByAdvertisementRef(UUID advertisementRef) {
        //get Advertisement
        Advertisement advertisement = advertisementRepository.findByAdvertisementRef(advertisementRef)
                .orElseThrow(() -> new AdvertisementNotFoundException(advertisementRef.toString()));

        return advertisement.getAdvertiser().getEmail();
    }

}
