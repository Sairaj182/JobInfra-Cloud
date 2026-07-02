package com.sairaj.jobinfra.server.tracking.service;

import com.sairaj.jobinfra.server.tracking.config.TrackingProperties;
import com.sairaj.jobinfra.server.tracking.core.TrackingContext.SessionInfo;
import com.sairaj.jobinfra.server.tracking.core.TrackingContext.VisitorInfo;
import com.sairaj.jobinfra.server.tracking.core.UuidVisitorIdentifier;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class VisitorIdServiceTest {

    private VisitorIdService visitorIdService;
    private TrackingProperties properties;
    private UuidVisitorIdentifier identifier;

    @BeforeEach
    void setUp() {
        properties = new TrackingProperties();
        identifier = new UuidVisitorIdentifier();
        visitorIdService = new VisitorIdService(properties, identifier);
    }

    @Test
    void shouldGenerateNewVisitorIdIfNoCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        VisitorInfo info = visitorIdService.processVisitor(request, response);

        assertTrue(info.newVisitor());
        assertNotNull(info.visitorId());

        Cookie cookie = response.getCookie("visitor_id");
        assertNotNull(cookie);
        assertEquals(info.visitorId(), cookie.getValue());
    }

    @Test
    void shouldReuseExistingVisitorId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String existingId = identifier.generateId();
        request.setCookies(new Cookie("visitor_id", existingId));
        MockHttpServletResponse response = new MockHttpServletResponse();

        VisitorInfo info = visitorIdService.processVisitor(request, response);

        assertFalse(info.newVisitor());
        assertEquals(existingId, info.visitorId());
        assertNull(response.getCookie("visitor_id"));
    }

    @Test
    void shouldGenerateNewSessionIfNoCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        SessionInfo info = visitorIdService.processSession(request, response);

        assertTrue(info.newSession());
        assertNotNull(info.sessionId());

        assertNotNull(response.getCookie("session_id"));
        assertNotNull(response.getCookie("last_seen"));
    }

    @Test
    void shouldExpireSessionIfLastSeenIsOld() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String oldSession = identifier.generateId();
        
        long oldLastSeen = Instant.now().getEpochSecond() - (35 * 60);
        request.setCookies(new Cookie("session_id", oldSession), new Cookie("last_seen", String.valueOf(oldLastSeen)));
        
        MockHttpServletResponse response = new MockHttpServletResponse();

        SessionInfo info = visitorIdService.processSession(request, response);

        assertTrue(info.newSession());
        assertNotEquals(oldSession, info.sessionId());
    }

    @Test
    void shouldReuseSessionIfLastSeenIsRecent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String activeSession = identifier.generateId();
        
        long recentLastSeen = Instant.now().getEpochSecond() - (10 * 60);
        request.setCookies(new Cookie("session_id", activeSession), new Cookie("last_seen", String.valueOf(recentLastSeen)));
        
        MockHttpServletResponse response = new MockHttpServletResponse();

        SessionInfo info = visitorIdService.processSession(request, response);

        assertFalse(info.newSession());
        assertEquals(activeSession, info.sessionId());
        
        assertNotNull(response.getCookie("last_seen"));
    }
}
