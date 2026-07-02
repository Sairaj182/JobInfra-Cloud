package com.sairaj.jobinfra.server.tracking.interceptor;

import com.sairaj.jobinfra.server.tracking.core.TrackingContext;
import com.sairaj.jobinfra.server.tracking.core.TrackingContext.BrowserInfo;
import com.sairaj.jobinfra.server.tracking.core.TrackingContext.SessionInfo;
import com.sairaj.jobinfra.server.tracking.core.TrackingContext.VisitorInfo;
import com.sairaj.jobinfra.server.tracking.service.BotDetectionService;
import com.sairaj.jobinfra.server.tracking.service.VisitorIdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VisitorIdInterceptorTest {

    private VisitorIdService visitorIdService;
    private BotDetectionService botDetectionService;
    private VisitorIdInterceptor interceptor;

    @BeforeEach
    void setUp() {
        visitorIdService = Mockito.mock(VisitorIdService.class);
        botDetectionService = Mockito.mock(BotDetectionService.class);
        interceptor = new VisitorIdInterceptor(visitorIdService, botDetectionService);
    }

    @Test
    void shouldProcessNormalVisitor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("User-Agent", "Mozilla");

        BrowserInfo browserInfo = new BrowserInfo("Mozilla", false, null);
        when(botDetectionService.detect("Mozilla")).thenReturn(browserInfo);

        VisitorInfo visitorInfo = new VisitorInfo("v-123", true);
        SessionInfo sessionInfo = new SessionInfo("s-456", true);

        when(visitorIdService.processVisitor(request, response)).thenReturn(visitorInfo);
        when(visitorIdService.processSession(request, response)).thenReturn(sessionInfo);

        boolean proceed = interceptor.preHandle(request, response, new Object());
        assertTrue(proceed);

        TrackingContext context = (TrackingContext) request.getAttribute(TrackingContext.ATTRIBUTE_NAME);
        assertNotNull(context);
        assertEquals("v-123", context.visitorInfo().visitorId());
        assertEquals("s-456", context.sessionInfo().sessionId());
        assertFalse(context.browserInfo().bot());
        assertNotNull(context.requestId());
    }

    @Test
    void shouldSkipIdsForBots() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("User-Agent", "Googlebot");

        BrowserInfo browserInfo = new BrowserInfo("Googlebot", true, "googlebot");
        when(botDetectionService.detect("Googlebot")).thenReturn(browserInfo);

        boolean proceed = interceptor.preHandle(request, response, new Object());
        assertTrue(proceed);

        TrackingContext context = (TrackingContext) request.getAttribute(TrackingContext.ATTRIBUTE_NAME);
        assertNotNull(context);
        assertTrue(context.browserInfo().bot());
        assertEquals("googlebot", context.browserInfo().botName());
        
        assertNull(context.visitorInfo());
        assertNull(context.sessionInfo());
        assertNotNull(context.requestId());

        verify(visitorIdService, never()).processVisitor(any(), any());
        verify(visitorIdService, never()).processSession(any(), any());
    }
}
