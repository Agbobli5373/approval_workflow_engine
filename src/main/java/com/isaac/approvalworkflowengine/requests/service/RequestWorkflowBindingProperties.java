package com.isaac.approvalworkflowengine.requests.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.requests")
public class RequestWorkflowBindingProperties {

    private Map<String, UUID> activeWorkflowVersions = new HashMap<>(
        Map.of("EXPENSE", UUID.fromString("11111111-1111-1111-1111-111111111111"))
    );

    public Map<String, UUID> getActiveWorkflowVersions() {
        return activeWorkflowVersions;
    }

    public void setActiveWorkflowVersions(Map<String, UUID> activeWorkflowVersions) {
        this.activeWorkflowVersions = activeWorkflowVersions;
    }
}
