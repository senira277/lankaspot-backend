package com.lankaspot.server.dto.advertiser.advertisement;

import java.math.BigDecimal;

public record QuoteSummaryDTO(
        String quoteRef,
        String title,
        String city,
        String district,
        String categoryName,
        BigDecimal price,
        String slug

) {
}
