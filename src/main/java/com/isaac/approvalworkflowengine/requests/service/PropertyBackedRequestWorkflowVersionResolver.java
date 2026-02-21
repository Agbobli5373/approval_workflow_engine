package com.isaac.approvalworkflowengine.requests.service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PropertyBackedRequestWorkflowVersionResolver implements RequestWorkflowVersionResolver {

    private final RequestWorkflowBindingProperties properties;

    public PropertyBackedRequestWorkflowVersionResolver(RequestWorkflowBindingProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<UUID> resolveActiveWorkflowVersionId(String requestType) {
        if (!StringUtils.hasText(requestType)) {
            return Optional.empty();
        }

        UUID directMatch = properties.getActiveWorkflowVersions().get(requestType);
        if (directMatch != null) {
            return Optional.of(directMatch);
        }

        return Optional.ofNullable(
            properties.getActiveWorkflowVersions().get(requestType.toUpperCase(Locale.ROOT))
        );
    }
}
