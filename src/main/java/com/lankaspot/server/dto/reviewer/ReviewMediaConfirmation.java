package com.lankaspot.server.dto.reviewer;

import com.lankaspot.server.enums.ReviewerApprovalStatus;

public record ReviewMediaConfirmation(
        Long id,
        ReviewerApprovalStatus reviewerApprovalStatus
) {
}
