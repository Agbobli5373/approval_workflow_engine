package com.isaac.approvalworkflowengine.requests.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AttachmentMetadata(
    @NotBlank String name,
    @NotBlank String contentType,
    @Min(1) long sizeBytes,
    String objectKey
) {
}
