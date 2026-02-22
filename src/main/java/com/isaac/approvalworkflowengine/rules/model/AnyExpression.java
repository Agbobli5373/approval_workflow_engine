package com.isaac.approvalworkflowengine.rules.model;

import java.util.List;

public record AnyExpression(List<RuleExpression> expressions) implements RuleExpression {
}
