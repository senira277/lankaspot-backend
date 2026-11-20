package com.lankaspot.server.dto.common;

import java.util.List;

public record FeatureDTO(
        Long quoteRef,
        List<FeatureDefDTO> features
) {
}
