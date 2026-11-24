package com.lankaspot.server.controller;

import com.lankaspot.server.dto.analytics.EngagementRequest;
import com.lankaspot.server.dto.analytics.EngagementStartResponse;
import com.lankaspot.server.dto.analytics.HeartbeatRequest;
import com.lankaspot.server.response.LankaspotResponse;
import com.lankaspot.server.service.AnalyticsIngestionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/record/{advertisementRef}/analytics")
@RequiredArgsConstructor
public class AdvertisementAnalyticsController {
    private final AnalyticsIngestionService analytics;

    @PostMapping("/view")
    public LankaspotResponse<Void> view(@PathVariable UUID advertisementRef,
                                        Authentication authentication,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        analytics.recordView(advertisementRef, authentication, request, response);
        return LankaspotResponse.success(null, "View recorded");
    }

    @PostMapping("/engagement/start")
    public LankaspotResponse<EngagementStartResponse> start(@PathVariable UUID advertisementRef,
                                                            Authentication authentication,
                                                            HttpServletRequest request,
                                                            HttpServletResponse response) {
        return LankaspotResponse.success(analytics.startEngagement(advertisementRef, authentication, request, response));
    }

    @PostMapping("/engagement/heartbeat")
    public LankaspotResponse<Void> heartbeat(@PathVariable UUID advertisementRef,
                                             @Valid @RequestBody HeartbeatRequest body,
                                             Authentication authentication,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        analytics.heartbeat(advertisementRef, body.sessionId(), body.intervalSeconds(), authentication, request, response);
        return LankaspotResponse.success(null, "Heartbeat accepted");
    }

    @PostMapping("/engagement/complete")
    public LankaspotResponse<Void> complete(@PathVariable UUID advertisementRef,
                                            @Valid @RequestBody EngagementRequest body,
                                            Authentication authentication,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        analytics.completeEngagement(advertisementRef, body.sessionId(), authentication, request, response);
        return LankaspotResponse.success(null, "Engagement recorded");
    }

    @PostMapping("/contact-retrieved")
    public LankaspotResponse<Void> contactRetrieved(@PathVariable UUID advertisementRef,
                                                    Authentication authentication,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) {
        analytics.recordContactRetrieval(advertisementRef, authentication, request, response);
        return LankaspotResponse.success(null, "Contact retrieval recorded");
    }
}

