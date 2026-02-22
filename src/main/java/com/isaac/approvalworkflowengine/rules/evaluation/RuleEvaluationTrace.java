package com.isaac.approvalworkflowengine.rules.evaluation;

public record RuleEvaluationTrace(
    String path,
    String expressionType,
    boolean result,
    String field,
    String operator,
    Object fieldValue,
    Object expectedValue,
    String reason
) {
}
