package com.lankaspot.server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "cookie_consents")
@Getter
@Setter
@NoArgsConstructor
public class CookieConsent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consent_id", nullable = false, length = 64)
    private String consentId;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private boolean necessary;

    @Column(nullable = false)
    private boolean analytics;

    @Column(nullable = false)
    private boolean marketing;

    @Column(name = "policy_version", nullable = false, length = 40)
    private String policyVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}

