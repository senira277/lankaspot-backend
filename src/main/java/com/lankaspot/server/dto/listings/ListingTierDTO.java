package com.lankaspot.server.dto.listings;

import java.time.LocalDateTime;
import java.util.UUID;

public record ListingTierDTO(
        UUID advertisementRef,
        Long id,
        String code,
        LocalDateTime createdAt,
        Integer durationDays
) {
}
