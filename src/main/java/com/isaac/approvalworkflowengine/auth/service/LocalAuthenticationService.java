package com.isaac.approvalworkflowengine.auth.service;

import com.isaac.approvalworkflowengine.auth.api.CurrentUserResponse;
import com.isaac.approvalworkflowengine.auth.api.LoginRequest;
import com.isaac.approvalworkflowengine.auth.api.LoginResponse;
import com.isaac.approvalworkflowengine.auth.model.UserAccount;
import com.isaac.approvalworkflowengine.auth.repository.TokenRevocationRepository;
import com.isaac.approvalworkflowengine.auth.repository.UserAccountRepository;
import com.isaac.approvalworkflowengine.auth.security.AppSecurityProperties;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.security", name = "mode", havingValue = "LOCAL_AUTH", matchIfMissing = true)
public class LocalAuthenticationService {

    private final UserAccountRepository userAccountRepository;
    private final TokenRevocationRepository tokenRevocationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final AppSecurityProperties securityProperties;

    public LocalAuthenticationService(
        UserAccountRepository userAccountRepository,
        TokenRevocationRepository tokenRevocationRepository,
        PasswordEncoder passwordEncoder,
        JwtEncoder jwtEncoder,
        AppSecurityProperties securityProperties
    ) {
        this.userAccountRepository = userAccountRepository;
        this.tokenRevocationRepository = tokenRevocationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.securityProperties = securityProperties;
    }

    public LoginResponse login(LoginRequest request) {
        UserAccount user = userAccountRepository.findByLoginIdentifier(request.usernameOrEmail().trim())
            .orElseThrow(() -> new AuthenticationFailedException("Invalid credentials"));

        if (!user.active() || user.passwordHash() == null || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new AuthenticationFailedException("Invalid credentials");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds((long) securityProperties.getJwt().getAccessTokenTtlMinutes() * 60);
        String jti = UUID.randomUUID().toString();

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
            .subject(user.externalSubject())
            .id(jti)
            .issuedAt(now)
            .expiresAt(expiresAt)
            .claim("uid", user.id().toString())
            .claim("email", user.email())
            .claim("displayName", user.displayName())
            .claim("roles", List.copyOf(user.roles()));

        if (user.department() != null && !user.department().isBlank()) {
            claimsBuilder.claim("department", user.department());
        }

        if (user.employeeId() != null && !user.employeeId().isBlank()) {
            claimsBuilder.claim("employeeId", user.employeeId());
        }

        String tokenValue = jwtEncoder.encode(
            JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claimsBuilder.build()
            )
        ).getTokenValue();

        return new LoginResponse(tokenValue, "Bearer", expiresAt);
    }

    public void logout(Jwt jwt) {
        if (jwt == null || jwt.getId() == null || jwt.getId().isBlank()) {
            throw new AuthenticationFailedException("Invalid token");
        }

        Instant expiresAt = jwt.getExpiresAt();
        tokenRevocationRepository.revoke(jwt.getId(), expiresAt != null ? expiresAt : Instant.now());
    }

    public CurrentUserResponse currentUser(Jwt jwt) {
        if (jwt == null) {
            throw new AuthenticationFailedException("Invalid token");
        }

        String subject = jwt.getSubject();

        UserAccount user = userAccountRepository.findByExternalSubject(subject)
            .orElseThrow(() -> new AuthenticationFailedException("User not found"));

        List<String> roles = user.roles().stream()
            .map(role -> role.toUpperCase(Locale.ROOT))
            .sorted()
            .toList();

        return new CurrentUserResponse(
            user.id(),
            user.externalSubject(),
            user.email(),
            user.displayName(),
            user.department(),
            user.employeeId(),
            roles
        );
    }
}
