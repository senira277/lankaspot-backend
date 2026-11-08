package com.lankaspot.server.dto.advertiser.advertisement;

import com.lankaspot.server.enums.AdvertisementStatus;

public record AdvertisementSummaryDTO(
        Boolean needAmendmentPropertyDetails,
        Boolean needAmendmentMedia,
        AdvertisementStatus status,
        String advertisementRef,
        QuoteSummaryDTO quote,
        MediaSummaryDTO media
) {
}
