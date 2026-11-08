package com.lankaspot.server.dto.reviewer.retrieve;

import com.lankaspot.server.dto.advertiser.retrieve.ContactDTO;
import com.lankaspot.server.dto.advertiser.retrieve.FeatureDefDTO;
import com.lankaspot.server.dto.advertiser.retrieve.MediaDefDTO;
import com.lankaspot.server.dto.advertiser.retrieve.QuoteDTO;

import java.util.List;

public record GetReviewerFullAdvertisement(
        String reason,
        String advertisementRef,
        QuoteDTO quote,
        List<MediaDefDTO> media,
        List<FeatureDefDTO> features,
        List<ContactDTO> contact
) {
}
