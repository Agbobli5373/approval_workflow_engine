package com.isaac.approvalworkflowengine.requests.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record RequestCreateInput(
    @NotBlank @Size(max = 80) String requestType,
    @NotBlank @Size(max = 200) String title,
    String description,
    @NotNull Map<String, Object> payload,
    BigDecimal amount,
    @Size(min = 3, max = 3) String currency,
    @Size(max = 80) String department,
    @Size(max = 80) String costCenter,
    List<@Valid AttachmentMetadata> attachments
) {
}
