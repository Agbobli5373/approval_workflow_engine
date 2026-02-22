package com.isaac.approvalworkflowengine.rules.api;

public record RuleEvaluationTraceResource(
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
