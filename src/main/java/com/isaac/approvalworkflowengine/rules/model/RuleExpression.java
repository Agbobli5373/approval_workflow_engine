package com.isaac.approvalworkflowengine.rules.model;

public sealed interface RuleExpression permits AllExpression, AnyExpression, NotExpression, PredicateExpression {
}
