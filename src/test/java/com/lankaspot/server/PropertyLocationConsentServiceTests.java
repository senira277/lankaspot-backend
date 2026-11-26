package com.lankaspot.server;

import com.lankaspot.server.model.ApplicationProperties;
import com.lankaspot.server.repository.ApplicationPropertiesRepository;
import com.lankaspot.server.service.PropertyLocationConsentService;
import com.lankaspot.server.service.PropertyService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PropertyLocationConsentServiceTests {

    @Test
    void retrievesMessageForHighestNumericConsentVersionFromApplicationProperties() {
        ApplicationPropertiesRepository repository =
                mock(ApplicationPropertiesRepository.class);
        PropertyService propertyService = mock(PropertyService.class);
        when(repository.findByKeyStartingWith("PropertyLocationConsent_"))
                .thenReturn(List.of(
                        property("PropertyLocationConsent_2"),
                        property("PropertyLocationConsent_10"),
                        property("PropertyLocationConsent_notice")
                ));
        when(propertyService.getValue("PropertyLocationConsent_10"))
                .thenReturn("Latest message");

        PropertyLocationConsentService.Consent consent =
                new PropertyLocationConsentService(repository, propertyService).latestConsent();

        assertEquals("10", consent.version());
        assertEquals("Latest message", consent.message());
    }

    @Test
    void failsClearlyWhenNoNumericVersionIsConfigured() {
        ApplicationPropertiesRepository repository =
                mock(ApplicationPropertiesRepository.class);
        PropertyService propertyService = mock(PropertyService.class);
        when(repository.findByKeyStartingWith("PropertyLocationConsent_"))
                .thenReturn(List.of(property("PropertyLocationConsent_notice")));

        assertThrows(
                IllegalStateException.class,
                () -> new PropertyLocationConsentService(repository, propertyService).latestConsent()
        );
    }

    private ApplicationProperties property(String key) {
        ApplicationProperties property = new ApplicationProperties();
        property.setKey(key);
        return property;
    }
}
