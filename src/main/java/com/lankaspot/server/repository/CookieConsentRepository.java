package com.lankaspot.server.repository;

import com.lankaspot.server.model.CookieConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CookieConsentRepository extends JpaRepository<CookieConsent, Long> {
    Optional<CookieConsent> findFirstByConsentIdOrderByCreatedAtDesc(String consentId);
    Optional<CookieConsent> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}

