package com.lankaspot.server.service;

import com.lankaspot.server.dto.analytics.ConsentRequest;
import com.lankaspot.server.dto.analytics.ConsentResponse;
import com.lankaspot.server.model.CookieConsent;
import com.lankaspot.server.repository.CookieConsentRepository;
import com.lankaspot.server.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CookieConsentService {
    public static final String CONSENT_COOKIE = "lankaspot_consent_id";

    private final CookieConsentRepository consentRepository;
    private final UserRepository userRepository;

    @Value("${lankaspot.consent.policy-version:v1}")
    private String policyVersion;

    @Value("${lankaspot.consent.expiration-days:365}")
    private int expirationDays;

    @Value("${lankaspot.analytics.cookie-secure:false}")
    private boolean cookieSecure;

    @Transactional(readOnly = true)
    public ConsentResponse getCurrent(Authentication authentication, HttpServletRequest request) {
        CookieConsent consent = null;
        if (isAuthenticated(authentication)) {
            consent = userRepository.findByKeycloakUserId(authentication.getName())
                    .flatMap(user -> consentRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId()))
                    .orElse(null);
        } else {
            String consentId = cookieValue(request, CONSENT_COOKIE);
            if (consentId != null) {
                consent = consentRepository.findFirstByConsentIdOrderByCreatedAtDesc(consentId).orElse(null);
            }
        }

        if (consent == null || consent.getExpiresAt().isBefore(Instant.now())
                || !policyVersion.equals(consent.getPolicyVersion())) {
            return new ConsentResponse(true, false, false, policyVersion, null, true);
        }
        return new ConsentResponse(true, consent.isAnalytics(), consent.isMarketing(),
                consent.getPolicyVersion(), consent.getExpiresAt(), false);
    }

    @Transactional
    public ConsentResponse save(ConsentRequest request, Authentication authentication,
                               HttpServletRequest servletRequest, HttpServletResponse response) {
        String consentId;
        Long userId = null;

        if (isAuthenticated(authentication)) {
            consentId = UUID.randomUUID().toString();
            userId = userRepository.findByKeycloakUserId(authentication.getName())
                    .map(user -> user.getId()).orElse(null);
        } else {
            consentId = cookieValue(servletRequest, CONSENT_COOKIE);
            if (consentId == null) consentId = UUID.randomUUID().toString();
        }

        Instant now = Instant.now();
        CookieConsent consent = new CookieConsent();
        consent.setConsentId(consentId);
        consent.setUserId(userId);
        consent.setNecessary(true);
        consent.setAnalytics(Boolean.TRUE.equals(request.analytics()));
        consent.setMarketing(Boolean.TRUE.equals(request.marketing()));
        consent.setPolicyVersion(policyVersion);
        consent.setCreatedAt(now);
        consent.setExpiresAt(now.plus(Duration.ofDays(expirationDays)));
        consentRepository.save(consent);

        if (!isAuthenticated(authentication)) {
            response.addHeader("Set-Cookie", consentCookie(consentId, consent.getExpiresAt()).toString());
        }

        return new ConsentResponse(true, consent.isAnalytics(), consent.isMarketing(),
                policyVersion, consent.getExpiresAt(), false);
    }

    @Transactional
    public ConsentResponse withdraw(Authentication authentication, HttpServletRequest request,
                                    HttpServletResponse response) {
        return save(new ConsentRequest(true, false, false), authentication, request, response);
    }

    public boolean analyticsAllowed(Authentication authentication, HttpServletRequest request) {
        return getCurrent(authentication, request).analytics();
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName());
    }

    private String cookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst().orElse(null);
    }

    private ResponseCookie consentCookie(String consentId, Instant expiresAt) {
        long maxAge = Math.max(0, Duration.between(Instant.now(), expiresAt).toSeconds());
        return ResponseCookie.from(CONSENT_COOKIE, consentId)
                .httpOnly(true).secure(cookieSecure).sameSite("Lax")
                .path("/").maxAge(Duration.ofSeconds(maxAge)).build();
    }
}

