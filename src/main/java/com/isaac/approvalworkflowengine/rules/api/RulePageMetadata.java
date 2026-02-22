package com.isaac.approvalworkflowengine.rules.api;

public record RulePageMetadata(
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
