package com.isaac.approvalworkflowengine.workflowruntime.api;

public record TaskPageMetadata(
    int number,
    int size,
    long totalElements,
    int totalPages
) {
}
