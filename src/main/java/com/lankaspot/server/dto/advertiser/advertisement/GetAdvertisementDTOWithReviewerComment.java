package com.lankaspot.server.dto.advertiser.advertisement;

public record GetAdvertisementDTOWithReviewerComment(
        AdvertisementDTO advertisement,
        String reason
) {
}
