package com.lankaspot.server.dto.reviewer.retrieve;

public record ReviewerResubmitAdvertisementSummary(
        String advertisementRef,
        String title,
        String reason
) {
}
