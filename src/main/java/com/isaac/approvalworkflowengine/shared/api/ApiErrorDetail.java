package com.isaac.approvalworkflowengine.shared.api;

/**
 * Field-level error detail for validation and request parsing failures.
 */
public record ApiErrorDetail(String field, String reason) {
}
