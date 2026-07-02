package com.sairaj.jobinfra.server.tracking.interceptor;

import com.sairaj.jobinfra.server.tracking.core.TrackingContext;
import com.sairaj.jobinfra.server.tracking.core.TrackingContext.BrowserInfo;
import com.sairaj.jobinfra.server.tracking.core.TrackingContext.SessionInfo;
import com.sairaj.jobinfra.server.tracking.core.TrackingContext.VisitorInfo;
import com.sairaj.jobinfra.server.tracking.service.BotDetectionService;
import com.sairaj.jobinfra.server.tracking.service.VisitorIdService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class VisitorIdInterceptor implements HandlerInterceptor {

    private final VisitorIdService visitorIdService;
    private final BotDetectionService botDetectionService;

    public VisitorIdInterceptor(VisitorIdService visitorIdService, BotDetectionService botDetectionService) {
        this.visitorIdService = visitorIdService;
        this.botDetectionService = botDetectionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userAgent = request.getHeader("User-Agent");
        BrowserInfo browserInfo = botDetectionService.detect(userAgent);

        TrackingContext.Builder contextBuilder = new TrackingContext.Builder()
                .requestId(UUID.randomUUID().toString())
                .browserInfo(browserInfo);

        if (!browserInfo.bot()) {
            VisitorInfo visitorInfo = visitorIdService.processVisitor(request, response);
            SessionInfo sessionInfo = visitorIdService.processSession(request, response);
            contextBuilder.visitorInfo(visitorInfo)
                          .sessionInfo(sessionInfo);
        }

        request.setAttribute(TrackingContext.ATTRIBUTE_NAME, contextBuilder.build());

        return true;
    }
}
