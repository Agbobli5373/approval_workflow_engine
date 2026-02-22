package com.isaac.approvalworkflowengine.rules;

public interface RuleSetLookup {

    boolean exists(String ruleSetKey, int versionNo);
}
