package com.lankaspot.server;

import com.lankaspot.server.dto.analytics.ConsentRequest;
import com.lankaspot.server.dto.analytics.ConsentResponse;
import com.lankaspot.server.model.CookieConsent;
import com.lankaspot.server.repository.CookieConsentRepository;
import com.lankaspot.server.repository.UserRepository;
import com.lankaspot.server.service.CookieConsentService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CookieConsentServiceTests {
    @Test
    void acceptanceAndRejectionArePersistedAsNewHistoryRecords() {
        CookieConsentRepository consentRepository = mock(CookieConsentRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        when(consentRepository.save(any(CookieConsent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CookieConsentService service = new CookieConsentService(consentRepository, userRepository);
        ReflectionTestUtils.setField(service, "policyVersion", "v1");
        ReflectionTestUtils.setField(service, "expirationDays", 365);
        ReflectionTestUtils.setField(service, "cookieSecure", false);

        MockHttpServletResponse response = new MockHttpServletResponse();
        ConsentResponse accepted = service.save(new ConsentRequest(true, true, false), null,
                new MockHttpServletRequest(), response);
        ConsentResponse rejected = service.withdraw(null,
                new MockHttpServletRequest(), new MockHttpServletResponse());

        assertTrue(accepted.analytics());
        assertFalse(rejected.analytics());
        verify(consentRepository, times(2)).save(any(CookieConsent.class));
    }

    @Test
    void currentConsentWithoutCookieRequestsRenewal() {
        CookieConsentRepository consentRepository = mock(CookieConsentRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CookieConsentService service = new CookieConsentService(consentRepository, userRepository);
        ReflectionTestUtils.setField(service, "policyVersion", "v2");

        ConsentResponse response = service.getCurrent(null, new MockHttpServletRequest());

        assertTrue(response.requiresConsentBanner());
        assertFalse(response.analytics());
    }
}

