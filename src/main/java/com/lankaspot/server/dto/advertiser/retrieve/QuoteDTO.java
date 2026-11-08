package com.lankaspot.server.dto.advertiser.retrieve;

import com.lankaspot.server.dto.advertiser.advertisement.FunctionalGroupDTO;
import com.lankaspot.server.enums.AdQuoteStatus;
import com.lankaspot.server.enums.AdSource;
import com.lankaspot.server.enums.ReviewerApprovalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


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
    String address,

    String coverImage,
    ReviewerApprovalStatus reviewerApprovalStatus

){}


