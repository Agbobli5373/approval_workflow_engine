package com.isaac.approvalworkflowengine.auth.policy;

public interface AccessPolicyService {

    AccessDecision canEditRequest(RequestAccessContext context);

    AccessDecision canDecideTask(TaskAccessContext context);

    AccessDecision canActivateWorkflow(WorkflowAccessContext context);
}
