package com.lankaspot.server.dto.advertiser.advertisement;


import java.util.List;

public record GetAllAdvertisementSummaryResponse(
        List<AdvertisementSummaryDTO> advertisements,
        Long page,
        Long size,
        Long totalPages,
        Long totalElements
) {
}
