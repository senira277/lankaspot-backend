package com.lankaspot.server.controller;

import com.lankaspot.server.dto.analytics.EngagementStartResponse;
import com.lankaspot.server.dto.listings.GetAllContactsOfListing;
import com.lankaspot.server.dto.listings.GetAllListingsResponse;
import com.lankaspot.server.dto.listings.GetFullAdvertisement;
import com.lankaspot.server.response.LankaspotResponse;
import com.lankaspot.server.service.AnalyticsIngestionService;
import com.lankaspot.server.service.ListingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
public class ListingsController {
    private final ListingsService listingsService;
    @GetMapping("/getAllListings")
    public LankaspotResponse<GetAllListingsResponse> getAllListings(
            @RequestParam(required = false) Long functionalGroupId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Long districtId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return LankaspotResponse.success(listingsService.getAllListings(
                functionalGroupId, categoryId, cityId, districtId, minPrice, maxPrice, page, size));
    }

    @GetMapping("/{advertisementRef}")
    public ResponseEntity<LankaspotResponse<GetFullAdvertisement>> getFullAdvertisement(@PathVariable UUID advertisementRef,Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(listingsService.getFullAdvertisement(advertisementRef, authentication, request, response));
    }

    @PostMapping("/{advertisementRef}/contact")
    public ResponseEntity<LankaspotResponse<GetAllContactsOfListing>> retrieveContact(@PathVariable UUID advertisementRef,
                                                                                      Authentication authentication,
                                                                                      HttpServletRequest request,
                                                                                      HttpServletResponse response) {
       return ResponseEntity.ok(listingsService.retrieveAdContact(advertisementRef, authentication, request, response));
    }

    @PostMapping("/{advertisementRef}/engagement/start")
    public ResponseEntity<LankaspotResponse<EngagementStartResponse>> startAdEngagement(@PathVariable UUID advertisementRef,
                                                                                        Authentication authentication,
                                                                                        HttpServletRequest request,
                                                                                        HttpServletResponse response) {
        return ResponseEntity.ok(listingsService.startAdEngagement(advertisementRef, authentication, request, response));
    }

    @PostMapping("/{advertisementRef}/engagement/heartbeat")
    public ResponseEntity<LankaspotResponse<Void>> recordAdHeartbeat(@PathVariable UUID advertisementRef,
                                                                     @RequestParam String sessionId,
                                                                     @RequestParam int intervalSeconds,
                                                                     Authentication authentication,
                                                                     HttpServletRequest request,
                                                                     HttpServletResponse response) {
        return ResponseEntity.ok(listingsService.recordAdHeartbeat(advertisementRef, sessionId, intervalSeconds, authentication, request, response));
    }

    @PostMapping("/{advertisementRef}/engagement/complete")
    public ResponseEntity<LankaspotResponse<Void>> completeAdEngagement(@PathVariable UUID advertisementRef,
                                                                        @RequestParam String sessionId,
                                                                        Authentication authentication,
                                                                        HttpServletRequest request,
                                                                        HttpServletResponse response) {
        return ResponseEntity.ok(listingsService.completeAdEngagement(advertisementRef, sessionId, authentication, request, response));
    }
}