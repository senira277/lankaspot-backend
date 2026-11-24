package com.lankaspot.server.dto.analytics;

import java.time.Instant;

public record ConsentResponse(
        boolean necessary,
        boolean analytics,
        boolean marketing,
        String policyVersion,
        Instant expiresAt,
        boolean requiresConsentBanner
) {
}

