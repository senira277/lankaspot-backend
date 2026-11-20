package com.lankaspot.server.dto.advertiser.retrieve;

import com.lankaspot.server.dto.common.FeatureDefDTO;
import com.lankaspot.server.dto.common.MediaDefDTO;
import com.lankaspot.server.dto.common.QuoteDTO;

import java.util.List;

public record GetResumeQuoteResponseData(
        QuoteDTO quote,
        List<MediaDefDTO> media,
        List<FeatureDefDTO> features,
        Integer lastCompletedStep
) {
}
