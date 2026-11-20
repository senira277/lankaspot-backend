package com.lankaspot.server.dto.advertiser.advertisement;

import com.lankaspot.server.dto.common.ContactDTO;
import com.lankaspot.server.dto.common.FeatureDefDTO;
import com.lankaspot.server.dto.common.MediaDefDTO;
import com.lankaspot.server.dto.common.QuoteDTO;
import com.lankaspot.server.enums.AdvertisementStatus;

import java.util.List;

public record AdvertisementDTO(
        AdvertisementStatus status,
        String advertisementRef,
        QuoteDTO quote,
        List<MediaDefDTO> media,
        List<FeatureDefDTO> features,
        List<ContactDTO> contact
) {
}
