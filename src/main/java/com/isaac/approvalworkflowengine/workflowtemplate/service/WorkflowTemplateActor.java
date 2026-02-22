package com.isaac.approvalworkflowengine.workflowtemplate.service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

public record WorkflowTemplateActor(
    UUID userId,
    String subject
) {

    public static WorkflowTemplateActor fromJwt(Jwt jwt) {
        String subject = jwt.getSubject();
        UUID userId = resolveUserId(jwt, subject);
        return new WorkflowTemplateActor(userId, subject);
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
}
