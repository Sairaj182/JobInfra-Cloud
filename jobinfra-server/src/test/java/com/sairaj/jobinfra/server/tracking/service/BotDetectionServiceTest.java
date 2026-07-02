package com.sairaj.jobinfra.server.tracking.service;

import com.sairaj.jobinfra.server.tracking.core.TrackingContext.BrowserInfo;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BotDetectionServiceTest {

    private final BotDetectionService botDetectionService = new BotDetectionService();

    @Test
    void shouldDetectGoogleBot() {
        BrowserInfo info = botDetectionService.detect("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)");
        assertTrue(info.bot());
        assertEquals("googlebot", info.botName());
    }

    @Test
    void shouldDetectGenericBot() {
        BrowserInfo info = botDetectionService.detect("Some Random Spider/1.0");
        assertTrue(info.bot());
        assertEquals("generic-bot", info.botName());
    }

    @Test
    void shouldNotDetectNormalBrowser() {
        BrowserInfo info = botDetectionService.detect("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        assertFalse(info.bot());
        assertNull(info.botName());
    }

    @Test
    void shouldHandleNullUserAgent() {
        BrowserInfo info = botDetectionService.detect(null);
        assertFalse(info.bot());
        assertNull(info.botName());
    }
}
