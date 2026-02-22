package com.isaac.approvalworkflowengine.rules.model;

import com.fasterxml.jackson.databind.JsonNode;

public record PredicateExpression(
    String field,
    RuleOperator operator,
    JsonNode value
) implements RuleExpression {
}
