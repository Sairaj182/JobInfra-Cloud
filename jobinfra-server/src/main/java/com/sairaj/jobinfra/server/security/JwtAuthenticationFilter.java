package com.sairaj.jobinfra.server.security;

import com.sairaj.jobinfra.server.service.AuditLogger;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuditLogger auditLogger;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService, AuditLogger auditLogger) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.auditLogger = auditLogger;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        String username = null;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        jwt = authHeader.substring(7);

        try {
            username = jwtService.extractUsername(jwt);
        } catch (ExpiredJwtException e) {
            auditLogger.logAuth("JWT_VALIDATION", "UNKNOWN", false, "Expired JWT");
        } catch (JwtException e) {
            auditLogger.logAuth("JWT_VALIDATION", "UNKNOWN", false, "Invalid JWT");
        } catch (Exception e) {
            auditLogger.logAuth("JWT_VALIDATION", "UNKNOWN", false, "JWT Processing Error: " + e.getMessage());
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            
            try {
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    auditLogger.logAuth("JWT_VALIDATION", username, false, "Invalid JWT for user");
                }
            } catch (Exception e) {
                auditLogger.logAuth("JWT_VALIDATION", username, false, "JWT Validation Error");
            }
        }
        filterChain.doFilter(request, response);
    }
}
