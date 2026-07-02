package com.sairaj.jobinfra.server.tracking.service;

import com.sairaj.jobinfra.server.tracking.config.TrackingProperties;
import com.sairaj.jobinfra.server.tracking.core.TrackingContext.SessionInfo;
import com.sairaj.jobinfra.server.tracking.core.TrackingContext.VisitorInfo;
import com.sairaj.jobinfra.server.tracking.core.VisitorIdentifier;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;

@Service
public class VisitorIdService {

    private final TrackingProperties properties;
    private final VisitorIdentifier identifier;

    public VisitorIdService(TrackingProperties properties, VisitorIdentifier identifier) {
        this.properties = properties;
        this.identifier = identifier;
    }

    public VisitorInfo processVisitor(HttpServletRequest request, HttpServletResponse response) {
        String visitorId = getCookieValue(request, properties.getVisitor().getCookieName());
        boolean isNew = false;

        if (!identifier.isValid(visitorId)) {
            visitorId = identifier.generateId();
            isNew = true;
            addCookie(response, properties.getVisitor().getCookieName(), visitorId, 
                      properties.getVisitor().getMaxAgeDays() * 24 * 60 * 60);
        }

        return new VisitorInfo(visitorId, isNew);
    }

    public SessionInfo processSession(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = getCookieValue(request, properties.getSession().getCookieName());
        String lastSeenStr = getCookieValue(request, properties.getSession().getLastSeenCookieName());
        
        boolean isNew = false;
        long now = Instant.now().getEpochSecond();
        boolean sessionExpired = false;

        if (lastSeenStr != null) {
            try {
                long lastSeen = Long.parseLong(lastSeenStr);
                long timeoutSeconds = properties.getSession().getTimeoutMinutes() * 60L;
                if (now - lastSeen > timeoutSeconds) {
                    sessionExpired = true;
                }
            } catch (NumberFormatException e) {
                sessionExpired = true;
            }
        } else if (sessionId != null) {
             sessionExpired = true;
        }

        if (!identifier.isValid(sessionId) || sessionExpired) {
            sessionId = identifier.generateId();
            isNew = true;
            addCookie(response, properties.getSession().getCookieName(), sessionId, -1);
        }

        addCookie(response, properties.getSession().getLastSeenCookieName(), String.valueOf(now), -1);

        return new SessionInfo(sessionId, isNew);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(properties.getVisitor().isSecureCookie());
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}
