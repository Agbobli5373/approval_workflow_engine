package com.isaac.approvalworkflowengine.requests.api;

import com.isaac.approvalworkflowengine.requests.model.RequestStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RequestResource(
    UUID id,
    String requestType,
    String title,
    String description,
    Map<String, Object> payload,
    BigDecimal amount,
    String currency,
    String department,
    String costCenter,
    List<AttachmentMetadata> attachments,
    RequestStatus status,
    UUID workflowVersionId,
    Instant createdAt,
    Instant updatedAt
) {
}
