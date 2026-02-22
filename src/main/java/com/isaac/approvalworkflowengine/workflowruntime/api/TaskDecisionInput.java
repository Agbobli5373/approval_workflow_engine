package com.isaac.approvalworkflowengine.workflowruntime.api;

import com.isaac.approvalworkflowengine.workflowruntime.model.TaskDecisionAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record TaskDecisionInput(
    @NotNull TaskDecisionAction action,
    @Size(max = 4000) String comment,
    UUID delegateUserId
) {
}
