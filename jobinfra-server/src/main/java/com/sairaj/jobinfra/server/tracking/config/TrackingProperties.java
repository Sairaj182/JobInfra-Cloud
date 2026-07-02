package com.sairaj.jobinfra.server.tracking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "tracking")
public class TrackingProperties {
    
    private final Visitor visitor = new Visitor();
    private final Session session = new Session();

    public Visitor getVisitor() {
        return visitor;
    }

    public Session getSession() {
        return session;
    }

    public static class Visitor {
        private String cookieName = "visitor_id";
        private boolean secureCookie = true;
        private int maxAgeDays = 365;

        public String getCookieName() {
            return cookieName;
        }

        public void setCookieName(String cookieName) {
            this.cookieName = cookieName;
        }

        public boolean isSecureCookie() {
            return secureCookie;
        }

        public void setSecureCookie(boolean secureCookie) {
            this.secureCookie = secureCookie;
        }

        public int getMaxAgeDays() {
            return maxAgeDays;
        }

        public void setMaxAgeDays(int maxAgeDays) {
            this.maxAgeDays = maxAgeDays;
        }
    }

    public static class Session {
        private String cookieName = "session_id";
        private String lastSeenCookieName = "last_seen";
        private int timeoutMinutes = 30;

        public String getCookieName() {
            return cookieName;
        }

        public void setCookieName(String cookieName) {
            this.cookieName = cookieName;
        }

        public String getLastSeenCookieName() {
            return lastSeenCookieName;
        }

        public void setLastSeenCookieName(String lastSeenCookieName) {
            this.lastSeenCookieName = lastSeenCookieName;
        }

        public int getTimeoutMinutes() {
            return timeoutMinutes;
        }

        public void setTimeoutMinutes(int timeoutMinutes) {
            this.timeoutMinutes = timeoutMinutes;
        }
    }
}
