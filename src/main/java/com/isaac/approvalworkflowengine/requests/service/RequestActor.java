package com.isaac.approvalworkflowengine.requests.service;

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

public record RequestActor(
    UUID userId,
    String subject,
    boolean workflowAdmin
) {

    public static RequestActor fromJwt(Jwt jwt) {
        String subject = jwt.getSubject();
        UUID userId = resolveUserId(jwt, subject);
        boolean workflowAdmin = extractRoles(jwt).contains("WORKFLOW_ADMIN");
        return new RequestActor(userId, subject, workflowAdmin);
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
}
