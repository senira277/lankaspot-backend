package com.lankaspot.server.dto.reviewer;

import com.lankaspot.server.enums.ApprovalStatus;

public record ReviewerConfirmationResponse(
        String advertisementRef,
        ApprovalStatus status,
        String reason
) {
}
