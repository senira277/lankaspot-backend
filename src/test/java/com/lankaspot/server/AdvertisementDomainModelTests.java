package com.lankaspot.server;

import com.lankaspot.server.enums.AdHistoryAction;
import com.lankaspot.server.model.AdQuote;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AdvertisementDomainModelTests {

    @Test
    void quoteStoresAContentSnapshotWithoutAEntityReference() {
        UUID ref = UUID.randomUUID();
        AdQuote quote = new AdQuote();
        quote.setAdvertisementRef(ref);
        quote.setVersion(1);
        quote.setTitle("House");
        quote.setSlug("house");
        quote.setDescription("Description");
        quote.setPrice(BigDecimal.TEN);
        quote.setTown("Colombo");
        quote.setNearestLandmark("Area");
        quote.setAddress("Address");
        quote.setCreatedAt(java.time.LocalDateTime.now());

        assertEquals(ref, quote.getAdvertisementRef());
        assertEquals(1, quote.getVersion());
        assertEquals("House", quote.getTitle());
        assertNotNull(quote.getCreatedAt());
    }

    @Test
    void amendmentAndRenewalAreExplicitHistoryActions() {
        assertEquals(AdHistoryAction.AMENDMENT, AdHistoryAction.valueOf("AMENDMENT"));
        assertEquals(AdHistoryAction.RENEWAL, AdHistoryAction.valueOf("RENEWAL"));
    }
}
