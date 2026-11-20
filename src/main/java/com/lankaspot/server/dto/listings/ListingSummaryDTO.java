package com.lankaspot.server.dto.listings;


import com.lankaspot.server.dto.advertiser.advertisement.MediaSummaryDTO;
import com.lankaspot.server.dto.advertiser.advertisement.FunctionalGroupDTO;
import com.lankaspot.server.dto.common.TierDTO;

import java.util.List;

public record ListingSummaryDTO(
        String views,
        String advertisementRef,
        FunctionalGroupDTO functionalGroup,
        ListingQuoteDTO quote,
        MediaSummaryDTO media,
        List<ListingFeatureDTO> features,
        List<TierDTO> tiers
) {
    public ListingSummaryDTO(String views, String advertisementRef,
                             FunctionalGroupDTO functionalGroup, ListingQuoteDTO quote, MediaSummaryDTO media) {
        this(views, advertisementRef, functionalGroup, quote, media, List.of(), List.of());
    }

    public ListingSummaryDTO(String views, String advertisementRef,
                             FunctionalGroupDTO functionalGroup, ListingQuoteDTO quote,
                             MediaSummaryDTO media, List<ListingFeatureDTO> features) {
        this(views, advertisementRef, functionalGroup, quote, media, features, List.of());
    }
}
