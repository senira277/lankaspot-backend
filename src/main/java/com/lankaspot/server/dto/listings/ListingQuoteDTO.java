package com.lankaspot.server.dto.listings;

import java.math.BigDecimal;

public record ListingQuoteDTO(
        String quoteRef,
        String title,
        String city,
        String district,
        String categoryName,
        BigDecimal price,
        String slug,
        String nearestLandmark,
        String town
) {
    public ListingQuoteDTO(String quoteRef, String title, String city, String district,
                           String categoryName, BigDecimal price, String slug,
                           String nearestLandmark) {
        this(quoteRef, title, city, district, categoryName, price, slug, nearestLandmark, null);
    }
}
