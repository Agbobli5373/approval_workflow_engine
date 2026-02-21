package com.isaac.approvalworkflowengine.auth.security;

import com.isaac.approvalworkflowengine.auth.ratelimit.RateLimitingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.core.convert.converter.Converter;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        AppSecurityProperties securityProperties,
        AuthenticationEntryPoint authenticationEntryPoint,
        AccessDeniedHandler accessDeniedHandler,
        Converter<Jwt, JwtAuthenticationToken> jwtAuthenticationConverter,
        RevokedTokenFilter revokedTokenFilter,
        RateLimitingFilter rateLimitingFilter
    ) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(authorize -> {
                authorize.requestMatchers("/actuator/health/liveness").permitAll();

                if (securityProperties.getMode() == SecurityMode.LOCAL_AUTH) {
                    authorize.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll();
                    authorize.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs", "/v3/api-docs.yaml")
                        .permitAll();
                } else {
                    authorize.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs", "/v3/api-docs.yaml")
                        .hasRole(SecurityRoles.WORKFLOW_ADMIN);
                }

                authorize.anyRequest().authenticated();
            })
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        http.addFilterAfter(revokedTokenFilter, BearerTokenAuthenticationFilter.class);
        http.addFilterAfter(rateLimitingFilter, RevokedTokenFilter.class);

        return http.build();
    }
}
