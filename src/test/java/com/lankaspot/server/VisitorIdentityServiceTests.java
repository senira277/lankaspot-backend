package com.lankaspot.server;

import com.lankaspot.server.repository.UserRepository;
import com.lankaspot.server.service.VisitorIdentityService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class VisitorIdentityServiceTests {
    @Test
    void authenticatedIdentityUsesSubjectAndNotAnonymousCookie() {
        VisitorIdentityService service = new VisitorIdentityService(mock(UserRepository.class));
        ReflectionTestUtils.setField(service, "hmacSecret", "test-secret");
        var auth = new UsernamePasswordAuthenticationToken("keycloak-user-123", null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_ADVERTISER")));

        var visitor = service.resolve(auth, new MockHttpServletRequest(), new MockHttpServletResponse());

        assertTrue(visitor.authenticated());
        assertEquals(64, visitor.visitorKeyHash().length());
        assertEquals("keycloak-user-123", visitor.rawIdentity());
    }

    @Test
    void anonymousVisitorRetainsCookieIdentity() {
        VisitorIdentityService service = new VisitorIdentityService(mock(UserRepository.class));
        ReflectionTestUtils.setField(service, "hmacSecret", "test-secret");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(VisitorIdentityService.VISITOR_COOKIE, "visitor-uuid-1"));

        var visitor = service.resolve(null, request, new MockHttpServletResponse());

        assertFalse(visitor.authenticated());
        assertEquals(64, visitor.visitorKeyHash().length());
    }
}

