package com.lankaspot.server.service.admin;

import com.lankaspot.server.enums.CacheInvalidationEventType;
import com.lankaspot.server.enums.EmailEventType;
import com.lankaspot.server.event.model.EmailSending.EmailEventBody;
import com.lankaspot.server.event.model.cacheInvalidation.CacheInvalidationBody;
import com.lankaspot.server.model.AdQuote;
import com.lankaspot.server.model.Advertisement;
import com.lankaspot.server.repository.AdvertisementRepository;
import com.lankaspot.server.response.LankaspotResponse;
import com.lankaspot.server.service.AdvertisementService;
import com.lankaspot.server.service.LiveAdCardService;
import com.lankaspot.server.service.kafka.CacheInvalidationProducer;
import com.lankaspot.server.service.kafka.EmailSendingProducer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminListingService {
    private final AdvertisementRepository advertisementRepository;
    private final AdvertisementService advertisementService;
    private final CacheInvalidationProducer cacheInvalidationProducer;
    private final EmailSendingProducer emailSendingProducer;
    private final LiveAdCardService liveAdCardService;

    public LankaspotResponse<Void> sendTestEmail() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Test User");
        variables.put("verificationLink", "https://lankaspot.lk/verify/test-token");

        emailSendingProducer.publish(new EmailEventBody(
                EmailEventType.ADVERTISEMENT_PURCHASED_SUCCESS,
                UUID.randomUUID(),
                variables
        ));

        return LankaspotResponse.success(null, "Test email sent successfully");
    }

    public LankaspotResponse<UUID> invalidateAdvertisementCaches(UUID advertisementRef, boolean refresh) {
        CacheInvalidationEventType eventType = refresh
                ? CacheInvalidationEventType.ADVERTISEMENT_FEATURES_TIERS_UPDATED
                : CacheInvalidationEventType.ADVERTISEMENT_FEATURES_TIERS_DELETED;

        cacheInvalidationProducer.publish(new CacheInvalidationBody(
                eventType,
                null,
                null,
                0L,
                advertisementRef
        ));

        return LankaspotResponse.success(
                advertisementRef,
                refresh
                        ? "Advertisement feature and tier cache refresh event published"
                        : "Advertisement feature and tier cache deletion event published"
        );
    }

    @Transactional
    public LankaspotResponse<UUID> cacheAdvertisement(UUID advertisementRef) {
        Advertisement advertisement = advertisementRepository.findByAdvertisementRef(advertisementRef)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Advertisement not found for the given reference"
                ));
        AdQuote quote = advertisementService.getQuoteByAdvertisementRef(advertisementRef);

        liveAdCardService.upsert(advertisement, quote);

        return LankaspotResponse.success(
                advertisementRef,
                "Advertisement cache refreshed"
        );
    }
}
