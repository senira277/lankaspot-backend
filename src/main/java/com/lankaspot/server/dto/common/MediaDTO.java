package com.lankaspot.server.dto.common;

import java.util.List;

public record MediaDTO(
        Long quoteRef,
        List<MediaDefDTO> media
) {
}
