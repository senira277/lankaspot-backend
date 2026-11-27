package com.lankaspot.server.dto.listings;

import java.util.UUID;

public record ListingFeatureDTO(UUID advertisementRef, String name, String value, String unit, String icon) {
    public ListingFeatureDTO(UUID advertisementRef, String name, String value, String unit) {
        this(advertisementRef, name, value, unit, null);
    }

    public ListingFeatureDTO(String name, String value, String unit) {
        this(null, name, value, unit, null);
    }

    public ListingFeatureDTO(String name, String value, String unit, String icon) {
        this(null, name, value, unit, icon);
    }
}
