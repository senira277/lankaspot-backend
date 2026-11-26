package com.lankaspot.server.dto.common;

import com.lankaspot.server.dto.advertiser.advertisement.FunctionalGroupDTO;
import com.lankaspot.server.enums.AdQuoteStatus;
import com.lankaspot.server.enums.AdSource;
import com.lankaspot.server.enums.ReviewerApprovalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public record QuoteDTO(

    //Functional group
    FunctionalGroupDTO functionalGroup,
    // Quote metadata
    Long id,
    Long quoteRef,
    Integer version,
    String slug,
    AdQuoteStatus status,
    AdSource source,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,

    // Property information
    Long categoryId,
    String categoryName,
    String title,
    String description,
    BigDecimal price,

    // Location
    Long cityId,
    String city,
    String district,
    Long districtId,
    String town,
    String nearestLandmark,
    BigDecimal distanceToNearestLandmarkInKm,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    boolean propertyLocationConsent,
    String propertyLocationConsentText,
    String propertyLocationConsentVersion,

    String coverImage,
    ReviewerApprovalStatus reviewerApprovalStatus

){}

