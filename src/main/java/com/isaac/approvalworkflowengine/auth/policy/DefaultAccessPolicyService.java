package com.isaac.approvalworkflowengine.auth.policy;

import com.isaac.approvalworkflowengine.auth.security.SecurityRoles;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DefaultAccessPolicyService implements AccessPolicyService {

    @Override
    public AccessDecision canEditRequest(RequestAccessContext context) {
        Set<String> roles = normalizeRoles(context.actorRoles());

        if (roles.contains(SecurityRoles.WORKFLOW_ADMIN)) {
            return AccessDecision.allow("ADMIN_OVERRIDE");
        }

        if (context.actorUserId() != null
            && context.requestorUserId() != null
            && context.actorUserId().equals(context.requestorUserId())
            && ("DRAFT".equals(context.requestStatus()) || "CHANGES_REQUESTED".equals(context.requestStatus()))) {
            return AccessDecision.allow("REQUESTOR_OWNS_EDITABLE_REQUEST");
        }

        return AccessDecision.deny("REQUEST_EDIT_NOT_ALLOWED");
    }

    @Override
    public AccessDecision canDecideTask(TaskAccessContext context) {
        Set<String> roles = normalizeRoles(context.actorRoles());

        if (roles.contains(SecurityRoles.WORKFLOW_ADMIN)) {
            return AccessDecision.allow("ADMIN_OVERRIDE");
        }

        if (context.taskDepartment() != null
            && context.actorDepartment() != null
            && !context.taskDepartment().equalsIgnoreCase(context.actorDepartment())) {
            return AccessDecision.deny("DEPARTMENT_MISMATCH");
        }

        if (context.assignedUserId() != null
            && context.actorUserId() != null
            && context.assignedUserId().equals(context.actorUserId())) {
            return AccessDecision.allow("DIRECT_ASSIGNMENT");
        }

        if (context.assignedRole() != null && roles.contains(context.assignedRole().toUpperCase(Locale.ROOT))) {
            return AccessDecision.allow("ROLE_ASSIGNMENT");
        }

        return AccessDecision.deny("TASK_DECISION_NOT_ALLOWED");
    }

    @Override
    public AccessDecision canActivateWorkflow(WorkflowAccessContext context) {
        Set<String> roles = normalizeRoles(context.actorRoles());

        if (roles.contains(SecurityRoles.WORKFLOW_ADMIN)) {
            return AccessDecision.allow("WORKFLOW_ADMIN");
        }

        return AccessDecision.deny("WORKFLOW_ACTIVATION_NOT_ALLOWED");
    }

    private Set<String> normalizeRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }

        return roles.stream()
            .map(role -> role.toUpperCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toSet());
    }
}
