package com.isaac.approvalworkflowengine.shared.api;

import java.util.List;

/**
 * Standard API error payload.
 */
public record ApiError(String code, String message, String correlationId, List<ApiErrorDetail> details) {

    public ApiError {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
