package com.sairaj.jobinfra.server.tracking.interceptor;

import com.sairaj.jobinfra.server.tracking.core.TrackingContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class VisitorLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(VisitorLoggingInterceptor.class);
    private static final String START_TIME_ATTR = "tracking_start_time";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.nanoTime();
        request.setAttribute(START_TIME_ATTR, startTime);

        TrackingContext context = (TrackingContext) request.getAttribute(TrackingContext.ATTRIBUTE_NAME);
        if (context != null) {
            if (context.requestId() != null) MDC.put("requestId", context.requestId());
            if (context.visitorInfo() != null) MDC.put("visitorId", context.visitorInfo().visitorId());
            if (context.sessionInfo() != null) MDC.put("sessionId", context.sessionInfo().sessionId());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
            long latencyNs = startTime != null ? System.nanoTime() - startTime : 0;
            long latencyMs = latencyNs / 1_000_000;

            TrackingContext context = (TrackingContext) request.getAttribute(TrackingContext.ATTRIBUTE_NAME);
            
            if (context != null) {
                String visitorId = context.visitorInfo() != null ? context.visitorInfo().visitorId() : null;
                String sessionId = context.sessionInfo() != null ? context.sessionInfo().sessionId() : null;
                Boolean newVisitor = context.visitorInfo() != null ? context.visitorInfo().newVisitor() : null;
                Boolean newSession = context.sessionInfo() != null ? context.sessionInfo().newSession() : null;
                boolean bot = context.browserInfo() != null && context.browserInfo().bot();
                String botName = bot ? context.browserInfo().botName() : null;

                log.info("Analytics Log - method={} endpoint={} status={} latencyMs={} bot={} botName={} visitorId={} sessionId={} newVisitor={} newSession={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        latencyMs,
                        bot,
                        botName,
                        visitorId,
                        sessionId,
                        newVisitor,
                        newSession
                );
            }
        } finally {
            MDC.clear();
        }
    }
}
