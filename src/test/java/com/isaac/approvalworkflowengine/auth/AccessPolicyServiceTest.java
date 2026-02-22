package com.isaac.approvalworkflowengine.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.isaac.approvalworkflowengine.auth.policy.AccessDecision;
import com.isaac.approvalworkflowengine.auth.policy.DefaultAccessPolicyService;
import com.isaac.approvalworkflowengine.auth.policy.RequestAccessContext;
import com.isaac.approvalworkflowengine.auth.policy.TaskAccessContext;
import com.isaac.approvalworkflowengine.auth.policy.WorkflowAccessContext;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccessPolicyServiceTest {

    private final DefaultAccessPolicyService accessPolicyService = new DefaultAccessPolicyService();

    @Test
    void canEditRequestAllowsRequestorInEditableStates() {
        UUID actorId = UUID.randomUUID();

        AccessDecision draftDecision = accessPolicyService.canEditRequest(
            new RequestAccessContext(actorId, Set.of("REQUESTOR"), actorId, "DRAFT")
        );

        AccessDecision changesRequestedDecision = accessPolicyService.canEditRequest(
            new RequestAccessContext(actorId, Set.of("REQUESTOR"), actorId, "CHANGES_REQUESTED")
        );

        assertThat(draftDecision.allowed()).isTrue();
        assertThat(changesRequestedDecision.allowed()).isTrue();
    }

    @Test
    void canEditRequestRejectsNonEditableStatusesForRequestor() {
        UUID actorId = UUID.randomUUID();

        AccessDecision decision = accessPolicyService.canEditRequest(
            new RequestAccessContext(actorId, Set.of("REQUESTOR"), actorId, "SUBMITTED")
        );

        assertThat(decision.allowed()).isFalse();
    }

    @Test
    void canDecideTaskAllowsDirectAssignee() {
        UUID actorId = UUID.randomUUID();

        AccessDecision decision = accessPolicyService.canDecideTask(
            new TaskAccessContext(actorId, Set.of("APPROVER"), actorId, null, "Finance", "Finance")
        );

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void canDecideTaskRejectsDepartmentMismatch() {
        UUID actorId = UUID.randomUUID();

        AccessDecision decision = accessPolicyService.canDecideTask(
            new TaskAccessContext(actorId, Set.of("APPROVER"), actorId, null, "IT", "Finance")
        );

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reasonCode()).isEqualTo("DEPARTMENT_MISMATCH");
    }

    @Test
    void canActivateWorkflowRequiresWorkflowAdminRole() {
        AccessDecision deniedDecision = accessPolicyService.canActivateWorkflow(
            new WorkflowAccessContext(Set.of("APPROVER"))
        );

        AccessDecision allowedDecision = accessPolicyService.canActivateWorkflow(
            new WorkflowAccessContext(Set.of("WORKFLOW_ADMIN"))
        );

        assertThat(deniedDecision.allowed()).isFalse();
        assertThat(allowedDecision.allowed()).isTrue();
    }
}
