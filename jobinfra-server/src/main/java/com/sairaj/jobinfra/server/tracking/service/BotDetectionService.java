package com.sairaj.jobinfra.server.tracking.service;

import com.sairaj.jobinfra.server.tracking.core.TrackingContext.BrowserInfo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BotDetectionService {

    private static final List<String> KNOWN_BOTS = List.of(
            "googlebot", "bingbot", "duckduckbot", "baiduspider", "yandexbot",
            "facebookbot", "linkedinbot", "slackbot", "discordbot",
            "curl", "wget", "java", "go-http-client", "python-requests",
            "shodan", "censys", "cms checker"
    );

    public BrowserInfo detect(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new BrowserInfo(userAgent, false, null);
        }

        String lowerUserAgent = userAgent.toLowerCase();
        
        for (String bot : KNOWN_BOTS) {
            if (lowerUserAgent.contains(bot)) {
                return new BrowserInfo(userAgent, true, bot);
            }
        }
        
        if (lowerUserAgent.contains("bot") || lowerUserAgent.contains("spider") || lowerUserAgent.contains("crawler")) {
             return new BrowserInfo(userAgent, true, "generic-bot");
        }

        return new BrowserInfo(userAgent, false, null);
    }
}
