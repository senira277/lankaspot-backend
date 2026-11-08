package com.lankaspot.server.dto.reviewer.retrieve;

import java.util.List;

public record ReviewerQueueDTO(
        List<ReviewerAdvertisementSummary> advertisements,
        Long page,
        Long size,
        Long totalPages,
        Long totalElements

) {
}
