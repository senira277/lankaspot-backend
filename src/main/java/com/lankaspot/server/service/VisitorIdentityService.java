package com.lankaspot.server.service;

import com.lankaspot.server.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VisitorIdentityService {
    public static final String VISITOR_COOKIE = "lankaspot_visitor_id";

    private final UserRepository userRepository;

    @Value("${lankaspot.analytics.hmac-secret:${random.value}}")
    private String hmacSecret;

    @Value("${lankaspot.analytics.cookie-secure:false}")
    private boolean cookieSecure;

    public ResolvedVisitor resolve(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            String userId = authentication.getName();
            return new ResolvedVisitor(hmac("user:" + userId), userId, true);
        }

        String visitorId = cookieValue(request, VISITOR_COOKIE);
        if (visitorId == null) {
            visitorId = UUID.randomUUID().toString();
            response.addHeader("Set-Cookie", visitorCookie(visitorId).toString());
        }
        return new ResolvedVisitor(hmac("visitor:" + visitorId), visitorId, false);
    }

    public String cookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    public String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate visitor key", e);
        }
    }

    private ResponseCookie visitorCookie(String value) {
        return ResponseCookie.from(VISITOR_COOKIE, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(365))
                .build();
    }

    public record ResolvedVisitor(String visitorKeyHash, String rawIdentity, boolean authenticated) {}
}
