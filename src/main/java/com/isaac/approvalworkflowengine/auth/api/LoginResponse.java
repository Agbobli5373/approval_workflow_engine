package com.isaac.approvalworkflowengine.auth.api;

import java.time.Instant;

public record LoginResponse(String accessToken, String tokenType, Instant expiresAt) {
}
