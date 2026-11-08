package com.lankaspot.server.dto.reviewer.retrieve;

import java.util.List;

public record GetReviewerResubmitQueueDTO(
        List<ReviewerResubmitAdvertisementSummary> advertisements,
        Long page,
        Long size,
        Long totalPages,
        Long totalElements
) {
}
