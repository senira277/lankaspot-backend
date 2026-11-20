package com.lankaspot.server.dto.listings;

import java.util.UUID;

public record ListingFeatureDTO(UUID advertisementRef, String name, String value, String unit) {
    public ListingFeatureDTO(String name, String value, String unit) {
        this(null, name, value, unit);
    }
}
