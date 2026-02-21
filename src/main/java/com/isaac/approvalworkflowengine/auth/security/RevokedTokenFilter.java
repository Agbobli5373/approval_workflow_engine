package com.isaac.approvalworkflowengine.auth.security;

import com.isaac.approvalworkflowengine.auth.repository.TokenRevocationRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RevokedTokenFilter extends OncePerRequestFilter {

    private final AppSecurityProperties securityProperties;
    private final TokenRevocationRepository tokenRevocationRepository;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public RevokedTokenFilter(
        AppSecurityProperties securityProperties,
        TokenRevocationRepository tokenRevocationRepository,
        AuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.securityProperties = securityProperties;
        this.tokenRevocationRepository = tokenRevocationRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        if (securityProperties.getMode() != SecurityMode.LOCAL_AUTH) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            String jti = jwtAuthenticationToken.getToken().getId();
            if (StringUtils.hasText(jti) && tokenRevocationRepository.isRevoked(jti)) {
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.commence(
                    request,
                    response,
                    new InsufficientAuthenticationException("Token has been revoked")
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
