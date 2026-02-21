package com.isaac.approvalworkflowengine.auth.ratelimit;

import com.isaac.approvalworkflowengine.auth.security.AppSecurityProperties;
import com.isaac.approvalworkflowengine.auth.security.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final AppSecurityProperties securityProperties;
    private final InMemoryRateLimiterService rateLimiterService;
    private final SecurityErrorResponseWriter errorResponseWriter;

    public RateLimitingFilter(
        AppSecurityProperties securityProperties,
        InMemoryRateLimiterService rateLimiterService,
        SecurityErrorResponseWriter errorResponseWriter
    ) {
        this.securityProperties = securityProperties;
        this.rateLimiterService = rateLimiterService;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!securityProperties.getRateLimit().isEnabled()) {
            return true;
        }

        return request.getRequestURI().equals("/actuator/health/liveness");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);

        int limit = authenticated
            ? securityProperties.getRateLimit().getAuthenticatedLimit()
            : securityProperties.getRateLimit().getAnonymousLimit();

        String key = resolveKey(request, authentication, authenticated);
        RateLimitDecision decision = rateLimiterService.consume(
            key,
            limit,
            securityProperties.getRateLimit().getWindowSeconds(),
            Instant.now()
        );

        if (!decision.allowed()) {
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            errorResponseWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Rate limit exceeded");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request, Authentication authentication, boolean authenticated) {
        if (authenticated && StringUtils.hasText(authentication.getName())) {
            return "user:" + authentication.getName();
        }

        String remoteAddress = request.getRemoteAddr();
        return "ip:" + (StringUtils.hasText(remoteAddress) ? remoteAddress : "unknown");
    }
}
