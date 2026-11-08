package com.lankaspot.server.dto.reviewer;

import com.lankaspot.server.enums.ApprovalStatus;

public record ReviewQuoteConfirmation(
        ApprovalStatus reviewerApprovalStatus
) {
}
