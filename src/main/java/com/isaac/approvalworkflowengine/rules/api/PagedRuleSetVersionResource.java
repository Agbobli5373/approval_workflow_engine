package com.isaac.approvalworkflowengine.rules.api;

import java.util.List;

public record PagedRuleSetVersionResource(
    List<RuleSetVersionResource> items,
    RulePageMetadata page
) {
}
