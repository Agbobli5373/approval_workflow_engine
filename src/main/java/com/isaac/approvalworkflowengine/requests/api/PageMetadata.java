package com.isaac.approvalworkflowengine.requests.api;

public record PageMetadata(
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
