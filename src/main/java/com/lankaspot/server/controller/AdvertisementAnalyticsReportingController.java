package com.lankaspot.server.controller;

import com.lankaspot.server.dto.analytics.AdvertisementAnalyticsResponse;
import com.lankaspot.server.response.LankaspotResponse;
import com.lankaspot.server.service.AdvertisementAnalyticsReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/report/{advertisementRef}/analytics")
@RequiredArgsConstructor
public class AdvertisementAnalyticsReportingController {
    private final AdvertisementAnalyticsReportingService reportingService;

    @GetMapping
    public LankaspotResponse<AdvertisementAnalyticsResponse> report(
            @PathVariable UUID advertisementRef,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        return LankaspotResponse.success(reportingService.report(advertisementRef, from, to, authentication));
    }
}

