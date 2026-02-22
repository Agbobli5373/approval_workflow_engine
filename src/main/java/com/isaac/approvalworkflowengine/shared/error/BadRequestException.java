package com.isaac.approvalworkflowengine.shared.error;

import com.isaac.approvalworkflowengine.shared.api.ApiErrorDetail;
import java.util.List;

public class BadRequestException extends RuntimeException {

    private final List<ApiErrorDetail> details;

    public BadRequestException(String message) {
        this(message, List.of());
    }

    public BadRequestException(String message, List<ApiErrorDetail> details) {
        super(message);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<ApiErrorDetail> details() {
        return details;
    }
}
