package com.lankaspot.server.dto.listings;

public record GetAllListingsResponse(
        ListingSummaryDTO[] listings,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
    public GetAllListingsResponse(ListingSummaryDTO[] listings) {
        this(listings, listings.length, listings.length == 0 ? 0 : 1, 0, listings.length);
    }
}
