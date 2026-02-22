package com.isaac.approvalworkflowengine.rules;

import com.isaac.approvalworkflowengine.rules.model.RuleEvaluationContext;

public interface RuleSetRuntimeEvaluator {

    boolean matches(String ruleSetKey, int versionNo, RuleEvaluationContext context);
}
