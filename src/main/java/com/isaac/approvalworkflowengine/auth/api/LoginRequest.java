package com.isaac.approvalworkflowengine.auth.api;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "usernameOrEmail is required") String usernameOrEmail,
    @NotBlank(message = "password is required") String password
) {
}
