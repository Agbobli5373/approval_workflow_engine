package com.isaac.approvalworkflowengine.workflowruntime.api;

import com.isaac.approvalworkflowengine.workflowruntime.model.TaskStatus;
import com.isaac.approvalworkflowengine.workflowruntime.service.TaskActor;
import com.isaac.approvalworkflowengine.workflowruntime.service.WorkflowRuntimeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(path = "/api/tasks", version = "1.0")
public class TaskController {

    private final WorkflowRuntimeService workflowRuntimeService;

    public TaskController(WorkflowRuntimeService workflowRuntimeService) {
        this.workflowRuntimeService = workflowRuntimeService;
    }

    @GetMapping
    public PagedTaskResource listTasks(
        @RequestParam(required = false) TaskAssignedToFilter assignedTo,
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return workflowRuntimeService.listTasks(
            TaskActor.fromJwt(jwt),
            assignedTo,
            status,
            page,
            size,
            sort
        );
    }

    @PostMapping("/{taskId}/claim")
    public TaskResource claimTask(
        @PathVariable UUID taskId,
        @RequestHeader("Idempotency-Key") @Size(min = 8, max = 120) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return workflowRuntimeService.claimTask(taskId, idempotencyKey, TaskActor.fromJwt(jwt));
    }

    @PostMapping("/{taskId}/decisions")
    public TaskDecisionResource decideTask(
        @PathVariable UUID taskId,
        @RequestHeader("Idempotency-Key") @Size(min = 8, max = 120) String idempotencyKey,
        @Valid @RequestBody TaskDecisionInput input,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return workflowRuntimeService.decideTask(taskId, idempotencyKey, input, TaskActor.fromJwt(jwt));
    }
}
