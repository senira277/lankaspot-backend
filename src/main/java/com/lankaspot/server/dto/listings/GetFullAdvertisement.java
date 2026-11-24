package com.lankaspot.server.dto.listings;

import com.lankaspot.server.dto.common.FeatureDefDTO;
import com.lankaspot.server.dto.common.MediaDefDTO;
import com.lankaspot.server.dto.common.QuoteDTO;

import java.util.List;

public record GetFullAdvertisement(
        QuoteDTO quote,
        List<FeatureDefDTO> features,
        List<MediaDefDTO> media

) {
}
