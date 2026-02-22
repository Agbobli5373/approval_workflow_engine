package com.isaac.approvalworkflowengine.rules.evaluation;

import java.util.List;

public record RuleEvaluationResult(
    boolean matched,
    List<RuleEvaluationTrace> traces
) {
}
