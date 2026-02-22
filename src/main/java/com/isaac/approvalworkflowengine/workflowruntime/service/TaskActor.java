package com.isaac.approvalworkflowengine.workflowruntime.service;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

public record TaskActor(
    UUID userId,
    String subject,
    Set<String> roles,
    String department
) {

    public static TaskActor fromJwt(Jwt jwt) {
        String subject = jwt.getSubject();
        UUID userId = resolveUserId(jwt, subject);
        return new TaskActor(
            userId,
            subject,
            extractRoles(jwt),
            trimToNull(jwt.getClaimAsString("department"))
        );
    }

    private static UUID resolveUserId(Jwt jwt, String subject) {
        Object uidClaim = jwt.getClaims().get("uid");
        if (uidClaim instanceof String uid && StringUtils.hasText(uid)) {
            try {
                return UUID.fromString(uid);
            } catch (IllegalArgumentException ignored) {
                // Fall through to deterministic UUID generation.
            }
        }

        String stableSource = StringUtils.hasText(subject) ? subject : "anonymous";
        return UUID.nameUUIDFromBytes(stableSource.getBytes(StandardCharsets.UTF_8));
    }

    private static Set<String> extractRoles(Jwt jwt) {
        Object rolesClaim = jwt.getClaims().get("roles");
        if (rolesClaim instanceof Collection<?> roleCollection) {
            return roleCollection.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        }

        Object realmAccessClaim = jwt.getClaims().get("realm_access");
        if (realmAccessClaim instanceof Map<?, ?> realmAccessMap) {
            Object realmRoles = realmAccessMap.get("roles");
            if (realmRoles instanceof Collection<?> roleCollection) {
                return roleCollection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(value -> value.toUpperCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            }
        }

        return Set.of();
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
