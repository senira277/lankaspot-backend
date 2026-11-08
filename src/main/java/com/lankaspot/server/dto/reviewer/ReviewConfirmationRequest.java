package com.lankaspot.server.dto.reviewer;

import com.lankaspot.server.enums.ApprovalStatus;

import java.util.List;

public record ReviewConfirmationRequest(
        String advertisementRef,
        ApprovalStatus status,
        String reason,
        List<ReviewMediaConfirmation> media,
        ReviewQuoteConfirmation quote

) {
}
