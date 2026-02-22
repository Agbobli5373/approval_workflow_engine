package com.isaac.approvalworkflowengine.rules.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RuleSetVersionResource(
    UUID id,
    String ruleSetKey,
    int versionNo,
    Map<String, Object> dsl,
    String checksumSha256,
    Instant createdAt
) {
}
