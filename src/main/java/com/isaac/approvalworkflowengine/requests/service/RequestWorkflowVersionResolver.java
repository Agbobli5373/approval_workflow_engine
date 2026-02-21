package com.isaac.approvalworkflowengine.requests.service;

import java.util.Optional;
import java.util.UUID;

public interface RequestWorkflowVersionResolver {

    Optional<UUID> resolveActiveWorkflowVersionId(String requestType);
}
