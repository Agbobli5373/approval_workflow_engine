package com.isaac.approvalworkflowengine.rules.api;

import java.util.List;

public record RuleSimulationResponse(
    String ruleSetKey,
    int versionNo,
    boolean matched,
    List<RuleEvaluationTraceResource> traces
) {
}
