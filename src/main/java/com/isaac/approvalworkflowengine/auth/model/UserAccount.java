package com.isaac.approvalworkflowengine.auth.model;

import java.util.Set;
import java.util.UUID;

public record UserAccount(
    UUID id,
    String externalSubject,
    String email,
    String displayName,
    String department,
    String employeeId,
    String passwordHash,
    boolean active,
    Set<String> roles
) {
}
