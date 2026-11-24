package com.lankaspot.server.controller;

import com.lankaspot.server.dto.analytics.ConsentRequest;
import com.lankaspot.server.dto.analytics.ConsentResponse;
import com.lankaspot.server.response.LankaspotResponse;
import com.lankaspot.server.service.CookieConsentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/consent")
@RequiredArgsConstructor
public class CookieConsentController {
    private final CookieConsentService consentService;

    @GetMapping("/status")
    public LankaspotResponse<ConsentResponse> getCurrent(Authentication authentication,
                                                         HttpServletRequest request) {
        return LankaspotResponse.success(consentService.getCurrent(authentication, request));
    }

    @PostMapping("/save")
    public LankaspotResponse<ConsentResponse> save(@Valid @RequestBody ConsentRequest body,
                                                   Authentication authentication,
                                                   HttpServletRequest request,
                                                   HttpServletResponse response) {
        return LankaspotResponse.success(consentService.save(body, authentication, request, response),
                "Consent preferences saved");
    }

    @PostMapping("/withdraw")
    public LankaspotResponse<ConsentResponse> withdraw(Authentication authentication,
                                                       HttpServletRequest request,
                                                       HttpServletResponse response) {
        return LankaspotResponse.success(consentService.withdraw(authentication, request, response),
                "Consent withdrawn");
    }
}

