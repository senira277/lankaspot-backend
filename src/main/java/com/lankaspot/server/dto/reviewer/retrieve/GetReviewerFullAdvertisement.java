package com.lankaspot.server.dto.reviewer.retrieve;

import com.lankaspot.server.dto.common.ContactDTO;
import com.lankaspot.server.dto.common.FeatureDefDTO;
import com.lankaspot.server.dto.common.MediaDefDTO;
import com.lankaspot.server.dto.common.QuoteDTO;

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
