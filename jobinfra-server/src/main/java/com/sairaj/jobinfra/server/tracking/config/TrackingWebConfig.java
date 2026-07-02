package com.sairaj.jobinfra.server.tracking.config;

import com.sairaj.jobinfra.server.tracking.interceptor.VisitorIdInterceptor;
import com.sairaj.jobinfra.server.tracking.interceptor.VisitorLoggingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class TrackingWebConfig implements WebMvcConfigurer {

    private final VisitorIdInterceptor visitorIdInterceptor;
    private final VisitorLoggingInterceptor visitorLoggingInterceptor;

    public TrackingWebConfig(VisitorIdInterceptor visitorIdInterceptor, VisitorLoggingInterceptor visitorLoggingInterceptor) {
        this.visitorIdInterceptor = visitorIdInterceptor;
        this.visitorLoggingInterceptor = visitorLoggingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(visitorIdInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/swagger-ui/**", "/v3/api-docs/**", "/error");
                
        registry.addInterceptor(visitorLoggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/swagger-ui/**", "/v3/api-docs/**", "/error");
    }
}
