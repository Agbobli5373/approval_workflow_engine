package com.isaac.approvalworkflowengine.auth.api;

import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
    UUID id,
    String username,
    String email,
    String displayName,
    String department,
    String employeeId,
    List<String> roles
) {
}
