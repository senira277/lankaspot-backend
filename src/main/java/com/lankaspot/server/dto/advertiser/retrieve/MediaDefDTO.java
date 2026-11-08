package com.lankaspot.server.dto.advertiser.retrieve;

import com.lankaspot.server.enums.MediaStatus;
import com.lankaspot.server.enums.MediaType;
import com.lankaspot.server.enums.ReviewerApprovalStatus;
import com.lankaspot.server.model.AdQuoteMedia;


public record MediaDefDTO(
        Long id,
        MediaType mediaType,
        String url,
        String publicId,
        Integer sortOrder,
        boolean primary,
        MediaStatus mediaStatus,
        ReviewerApprovalStatus reviewerApprovalStatus
) {
    public static MediaDefDTO from(AdQuoteMedia media) {
        return new MediaDefDTO(
                media.getId(),
                media.getMediaType(),
                media.getUrl(),
                media.getPublicId(),
                media.getSortOrder(),
                media.isPrimary(),
                media.getMediaStatus(),
                media.getReviewerApprovalStatus()
        );
    }
}
