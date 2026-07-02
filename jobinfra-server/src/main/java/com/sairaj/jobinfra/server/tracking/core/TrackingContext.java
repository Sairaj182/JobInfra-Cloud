package com.sairaj.jobinfra.server.tracking.core;

import java.time.Instant;

/**
 * Immutable request-scoped context containing all tracking information.
 */
public record TrackingContext(
    VisitorInfo visitorInfo,
    SessionInfo sessionInfo,
    GeoInfo geoInfo,
    BrowserInfo browserInfo,
    DeviceInfo deviceInfo,
    SecurityInfo securityInfo,
    String requestId
) {
    public static final String ATTRIBUTE_NAME = "trackingContext";

    public record VisitorInfo(String visitorId, boolean newVisitor) {}
    
    public record SessionInfo(String sessionId, boolean newSession) {}
    
    public record GeoInfo(String city, String country, String ipAddress) {}
    
    public record BrowserInfo(String userAgent, boolean bot, String botName) {}
    
    public record DeviceInfo(String os, String deviceType) {}
    
    public record SecurityInfo(boolean suspicious) {}

    /**
     * Builder for TrackingContext to easily create immutable copies with enriched data.
     */
    public static class Builder {
        private VisitorInfo visitorInfo;
        private SessionInfo sessionInfo;
        private GeoInfo geoInfo;
        private BrowserInfo browserInfo;
        private DeviceInfo deviceInfo;
        private SecurityInfo securityInfo;
        private String requestId;

        public Builder() {}

        public Builder(TrackingContext context) {
            this.visitorInfo = context.visitorInfo();
            this.sessionInfo = context.sessionInfo();
            this.geoInfo = context.geoInfo();
            this.browserInfo = context.browserInfo();
            this.deviceInfo = context.deviceInfo();
            this.securityInfo = context.securityInfo();
            this.requestId = context.requestId();
        }

        public Builder visitorInfo(VisitorInfo visitorInfo) { this.visitorInfo = visitorInfo; return this; }
        public Builder sessionInfo(SessionInfo sessionInfo) { this.sessionInfo = sessionInfo; return this; }
        public Builder geoInfo(GeoInfo geoInfo) { this.geoInfo = geoInfo; return this; }
        public Builder browserInfo(BrowserInfo browserInfo) { this.browserInfo = browserInfo; return this; }
        public Builder deviceInfo(DeviceInfo deviceInfo) { this.deviceInfo = deviceInfo; return this; }
        public Builder securityInfo(SecurityInfo securityInfo) { this.securityInfo = securityInfo; return this; }
        public Builder requestId(String requestId) { this.requestId = requestId; return this; }

        public TrackingContext build() {
            return new TrackingContext(visitorInfo, sessionInfo, geoInfo, browserInfo, deviceInfo, securityInfo, requestId);
        }
    }
}
