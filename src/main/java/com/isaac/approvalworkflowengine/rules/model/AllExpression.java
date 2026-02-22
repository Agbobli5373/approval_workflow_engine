package com.isaac.approvalworkflowengine.rules.model;

import java.util.List;

public record AllExpression(List<RuleExpression> expressions) implements RuleExpression {
}
