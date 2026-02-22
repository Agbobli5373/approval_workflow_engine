package com.isaac.approvalworkflowengine.workflowruntime.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isaac.approvalworkflowengine.auth.policy.AccessDecision;
import com.isaac.approvalworkflowengine.auth.policy.AccessPolicyService;
import com.isaac.approvalworkflowengine.auth.policy.TaskAccessContext;
import com.isaac.approvalworkflowengine.rules.RuleSetRuntimeEvaluator;
import com.isaac.approvalworkflowengine.rules.model.RuleEvaluationContext;
import com.isaac.approvalworkflowengine.shared.api.ApiErrorDetail;
import com.isaac.approvalworkflowengine.shared.error.BadRequestException;
import com.isaac.approvalworkflowengine.workflowruntime.api.PagedTaskResource;
import com.isaac.approvalworkflowengine.workflowruntime.api.TaskAssignedToFilter;
import com.isaac.approvalworkflowengine.workflowruntime.api.TaskDecisionInput;
import com.isaac.approvalworkflowengine.workflowruntime.api.TaskDecisionResource;
import com.isaac.approvalworkflowengine.workflowruntime.api.TaskPageMetadata;
import com.isaac.approvalworkflowengine.workflowruntime.api.TaskResource;
import com.isaac.approvalworkflowengine.workflowruntime.execution.WorkflowRuntimeGraph;
import com.isaac.approvalworkflowengine.workflowruntime.model.RuntimeRequestStatus;
import com.isaac.approvalworkflowengine.workflowruntime.model.TaskDecisionAction;
import com.isaac.approvalworkflowengine.workflowruntime.model.TaskStatus;
import com.isaac.approvalworkflowengine.workflowruntime.model.WorkflowInstanceStatus;
import com.isaac.approvalworkflowengine.workflowruntime.repository.RuntimeIdempotencyKeyJpaRepository;
import com.isaac.approvalworkflowengine.workflowruntime.repository.RuntimeRequestJpaRepository;
import com.isaac.approvalworkflowengine.workflowruntime.repository.RuntimeRequestStatusTransitionJpaRepository;
import com.isaac.approvalworkflowengine.workflowruntime.repository.TaskDecisionJpaRepository;
import com.isaac.approvalworkflowengine.workflowruntime.repository.TaskJpaRepository;
import com.isaac.approvalworkflowengine.workflowruntime.repository.WorkflowInstanceJpaRepository;
import com.isaac.approvalworkflowengine.workflowruntime.repository.entity.RuntimeIdempotencyKeyEntity;
import com.isaac.approvalworkflowengine.workflowruntime.repository.entity.RuntimeRequestEntity;
import com.isaac.approvalworkflowengine.workflowruntime.repository.entity.RuntimeRequestStatusTransitionEntity;
import com.isaac.approvalworkflowengine.workflowruntime.repository.entity.TaskDecisionEntity;
import com.isaac.approvalworkflowengine.workflowruntime.repository.entity.TaskEntity;
import com.isaac.approvalworkflowengine.workflowruntime.repository.entity.WorkflowInstanceEntity;
import com.isaac.approvalworkflowengine.workflowtemplate.WorkflowTemplateRuntimeLookup;
import com.isaac.approvalworkflowengine.workflowtemplate.WorkflowTemplateRuntimeLookup.WorkflowTemplateRuntimeVersion;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowAssignmentInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowGraphInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowJoinInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowNodeInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowRuleRefInput;
import com.isaac.approvalworkflowengine.workflowtemplate.model.WorkflowAssignmentStrategy;
import com.isaac.approvalworkflowengine.workflowtemplate.model.WorkflowJoinPolicy;
import com.isaac.approvalworkflowengine.workflowtemplate.model.WorkflowNodeType;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WorkflowRuntimeService {

    private static final String TASK_CLAIM_SCOPE = "TASK_CLAIM";

    private static final Set<TaskStatus> ACTIVE_TASK_STATUSES = EnumSet.of(TaskStatus.PENDING, TaskStatus.CLAIMED);
    private static final Set<TaskStatus> TERMINAL_TASK_STATUSES = EnumSet.of(
        TaskStatus.APPROVED,
        TaskStatus.REJECTED,
        TaskStatus.CANCELLED,
        TaskStatus.EXPIRED,
        TaskStatus.SKIPPED
    );

    private static final Map<String, String> SORT_FIELD_MAPPING = Map.of(
        "createdAt", "createdAt",
        "updatedAt", "updatedAt",
        "dueAt", "dueAt",
        "status", "status",
        "stepKey", "stepKey"
    );

    private final WorkflowInstanceJpaRepository workflowInstanceJpaRepository;
    private final TaskJpaRepository taskJpaRepository;
    private final TaskDecisionJpaRepository taskDecisionJpaRepository;
    private final RuntimeRequestJpaRepository runtimeRequestJpaRepository;
    private final RuntimeRequestStatusTransitionJpaRepository runtimeRequestStatusTransitionJpaRepository;
    private final RuntimeIdempotencyKeyJpaRepository runtimeIdempotencyKeyJpaRepository;
    private final WorkflowTemplateRuntimeLookup workflowTemplateRuntimeLookup;
    private final RuleSetRuntimeEvaluator ruleSetRuntimeEvaluator;
    private final AccessPolicyService accessPolicyService;
    private final ObjectMapper objectMapper;

    public WorkflowRuntimeService(
        WorkflowInstanceJpaRepository workflowInstanceJpaRepository,
        TaskJpaRepository taskJpaRepository,
        TaskDecisionJpaRepository taskDecisionJpaRepository,
        RuntimeRequestJpaRepository runtimeRequestJpaRepository,
        RuntimeRequestStatusTransitionJpaRepository runtimeRequestStatusTransitionJpaRepository,
        RuntimeIdempotencyKeyJpaRepository runtimeIdempotencyKeyJpaRepository,
        WorkflowTemplateRuntimeLookup workflowTemplateRuntimeLookup,
        RuleSetRuntimeEvaluator ruleSetRuntimeEvaluator,
        AccessPolicyService accessPolicyService,
        ObjectMapper objectMapper
    ) {
        this.workflowInstanceJpaRepository = workflowInstanceJpaRepository;
        this.taskJpaRepository = taskJpaRepository;
        this.taskDecisionJpaRepository = taskDecisionJpaRepository;
        this.runtimeRequestJpaRepository = runtimeRequestJpaRepository;
        this.runtimeRequestStatusTransitionJpaRepository = runtimeRequestStatusTransitionJpaRepository;
        this.runtimeIdempotencyKeyJpaRepository = runtimeIdempotencyKeyJpaRepository;
        this.workflowTemplateRuntimeLookup = workflowTemplateRuntimeLookup;
        this.ruleSetRuntimeEvaluator = ruleSetRuntimeEvaluator;
        this.accessPolicyService = accessPolicyService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RuntimeBootstrapResult startOrRestart(UUID requestId, String actorSubject) {
        RuntimeRequestEntity request = runtimeRequestJpaRepository.findByIdForUpdate(requestId)
            .orElseThrow(() -> new NoSuchElementException("Request not found"));

        UUID workflowVersionId = request.getWorkflowVersionId();
        if (workflowVersionId == null) {
            throw new IllegalStateException("Request has no bound workflow version");
        }

        WorkflowTemplateRuntimeVersion runtimeVersion = workflowTemplateRuntimeLookup
            .findRuntimeWorkflowVersion(workflowVersionId)
            .orElseThrow(() -> new IllegalStateException("Workflow version not found for runtime execution"));

        WorkflowRuntimeGraph graph = WorkflowRuntimeGraph.from(runtimeVersion.graph());
        WorkflowInstanceEntity instance = workflowInstanceJpaRepository.findByRequestIdForUpdate(requestId)
            .orElseGet(() -> createInstance(requestId, workflowVersionId));

        if (instance.getId() != null) {
            resetInstance(instance, workflowVersionId);
        }

        RuntimeProgressResult result = processFromNodes(
            instance,
            request,
            graph,
            graph.successorKeys(graph.startNode().id().trim()),
            actorSubject
        );

        RuntimeRequestStatus targetStatus = result.terminalRequestStatus() == null
            ? RuntimeRequestStatus.IN_REVIEW
            : result.terminalRequestStatus();

        return new RuntimeBootstrapResult(targetStatus);
    }

    @Transactional
    public void cancelActiveRuntime(UUID requestId) {
        WorkflowInstanceEntity instance = workflowInstanceJpaRepository.findByRequestIdForUpdate(requestId)
            .orElse(null);

        if (instance == null) {
            return;
        }

        if (instance.getStatus() != WorkflowInstanceStatus.ACTIVE) {
            return;
        }

        List<TaskEntity> activeTasks = taskJpaRepository.findByWorkflowInstanceIdAndStatusIn(
            instance.getId(),
            ACTIVE_TASK_STATUSES
        );

        for (TaskEntity task : activeTasks) {
            task.setStatus(TaskStatus.CANCELLED);
        }

        taskJpaRepository.saveAll(activeTasks);

        instance.setStatus(WorkflowInstanceStatus.CANCELLED);
        instance.setCurrentStepKeys(writeJson(List.of()));
        workflowInstanceJpaRepository.save(instance);
    }

    @Transactional(readOnly = true)
    public PagedTaskResource listTasks(
        TaskActor actor,
        TaskAssignedToFilter assignedTo,
        TaskStatus status,
        int page,
        int size,
        String sort
    ) {
        Pageable pageable = PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), 200),
            parseSort(sort)
        );

        Specification<TaskEntity> specification = (root, query, builder) -> builder.conjunction();

        if (status != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("status"), status));
        }

        Specification<TaskEntity> assignmentSpec = assignmentSpecification(actor, assignedTo);
        specification = specification.and(assignmentSpec);

        Page<TaskEntity> taskPage = taskJpaRepository.findAll(specification, pageable);
        List<TaskResource> items = taskPage.getContent().stream().map(this::toTaskResource).toList();

        return new PagedTaskResource(
            items,
            new TaskPageMetadata(
                taskPage.getNumber(),
                taskPage.getSize(),
                taskPage.getTotalElements(),
                taskPage.getTotalPages()
            )
        );
    }

    @Transactional
    public TaskResource claimTask(UUID taskId, String idempotencyKey, TaskActor actor) {
        String requestHash = computeRequestHash(TASK_CLAIM_SCOPE, taskId, actor.userId());

        RuntimeIdempotencyKeyEntity existingIdempotency = runtimeIdempotencyKeyJpaRepository
            .findByScopeAndKeyValue(TASK_CLAIM_SCOPE, idempotencyKey)
            .orElse(null);

        if (existingIdempotency != null) {
            ensureMatchingRequestHash(existingIdempotency, requestHash);
            TaskEntity task = taskJpaRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found"));
            return toTaskResource(task);
        }

        TaskEntity task = taskJpaRepository.findByIdForUpdate(taskId)
            .orElseThrow(() -> new NoSuchElementException("Task not found"));

        RuntimeRequestEntity request = runtimeRequestJpaRepository.findById(task.getRequestId())
            .orElseThrow(() -> new NoSuchElementException("Request not found"));

        authorizeTaskAction(task, request, actor);

        if (task.getStatus() == TaskStatus.PENDING) {
            task.setStatus(TaskStatus.CLAIMED);
            task.setClaimedByUserId(actor.userId());
            task.setClaimedAt(Instant.now());
            taskJpaRepository.save(task);
        } else if (task.getStatus() == TaskStatus.CLAIMED && Objects.equals(task.getClaimedByUserId(), actor.userId())) {
            // Idempotent claim from same actor.
        } else {
            throw new IllegalStateException("Task cannot be claimed in status " + task.getStatus());
        }

        RuntimeIdempotencyKeyEntity record = new RuntimeIdempotencyKeyEntity(
            UUID.randomUUID(),
            TASK_CLAIM_SCOPE,
            idempotencyKey,
            requestHash,
            writeJson(Map.of("taskId", task.getId().toString(), "status", task.getStatus().name())),
            Instant.now()
        );

        try {
            runtimeIdempotencyKeyJpaRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException conflict) {
            RuntimeIdempotencyKeyEntity persisted = runtimeIdempotencyKeyJpaRepository
                .findByScopeAndKeyValue(TASK_CLAIM_SCOPE, idempotencyKey)
                .orElseThrow(() -> conflict);
            ensureMatchingRequestHash(persisted, requestHash);
        }

        return toTaskResource(task);
    }

    @Transactional
    public TaskDecisionResource decideTask(UUID taskId, String idempotencyKey, TaskDecisionInput input, TaskActor actor) {
        TaskDecisionEntity existingDecision = taskDecisionJpaRepository
            .findByTaskIdAndIdempotencyKey(taskId, idempotencyKey)
            .orElse(null);

        if (existingDecision != null) {
            ensureMatchingDecision(existingDecision, input, actor);
            return toTaskDecisionResource(existingDecision);
        }

        TaskEntity task = taskJpaRepository.findByIdForUpdate(taskId)
            .orElseThrow(() -> new NoSuchElementException("Task not found"));

        RuntimeRequestEntity request = runtimeRequestJpaRepository.findByIdForUpdate(task.getRequestId())
            .orElseThrow(() -> new NoSuchElementException("Request not found"));

        WorkflowInstanceEntity instance = workflowInstanceJpaRepository.findByIdForUpdate(task.getWorkflowInstanceId())
            .orElseThrow(() -> new NoSuchElementException("Workflow instance not found"));

        authorizeTaskAction(task, request, actor);

        if (task.getStatus() != TaskStatus.CLAIMED || !Objects.equals(task.getClaimedByUserId(), actor.userId())) {
            throw new IllegalStateException("Task must be claimed by the acting user before deciding");
        }

        TaskDecisionAction action = input.action();
        if (action == TaskDecisionAction.DELEGATE) {
            throw new IllegalStateException("DELEGATE action is not supported in E5");
        }

        TaskDecisionEntity decisionEntity = new TaskDecisionEntity();
        decisionEntity.setId(UUID.randomUUID());
        decisionEntity.setTaskId(taskId);
        decisionEntity.setAction(action);
        decisionEntity.setComment(trimToNull(input.comment()));
        decisionEntity.setActedByUserId(actor.userId());
        decisionEntity.setActedOnBehalfOfUserId(null);
        decisionEntity.setIdempotencyKey(idempotencyKey);

        if (action == TaskDecisionAction.APPROVE) {
            task.setStatus(TaskStatus.APPROVED);
            taskJpaRepository.save(task);

            WorkflowTemplateRuntimeVersion runtimeVersion = workflowTemplateRuntimeLookup
                .findRuntimeWorkflowVersion(instance.getWorkflowVersionId())
                .orElseThrow(() -> new IllegalStateException("Workflow version not found for runtime execution"));

            WorkflowRuntimeGraph graph = WorkflowRuntimeGraph.from(runtimeVersion.graph());
            RuntimeProgressResult progressResult = processFromNodes(
                instance,
                request,
                graph,
                graph.successorKeys(task.getStepKey()),
                actor.subject()
            );

            if (progressResult.terminalRequestStatus() == RuntimeRequestStatus.APPROVED) {
                transitionRequestStatus(request, RuntimeRequestStatus.APPROVED, actor.subject(), "TASK_APPROVE");
            } else if (request.getStatus() != RuntimeRequestStatus.IN_REVIEW) {
                transitionRequestStatus(request, RuntimeRequestStatus.IN_REVIEW, actor.subject(), "TASK_APPROVE");
            }
        } else {
            ensureDecisionCommentRequired(action, input.comment());

            task.setStatus(TaskStatus.REJECTED);
            taskJpaRepository.save(task);

            RuntimeRequestStatus targetStatus = action == TaskDecisionAction.REJECT
                ? RuntimeRequestStatus.REJECTED
                : RuntimeRequestStatus.CHANGES_REQUESTED;

            cancelSiblingActiveTasks(task);
            instance.setStatus(action == TaskDecisionAction.REJECT
                ? WorkflowInstanceStatus.REJECTED
                : WorkflowInstanceStatus.CHANGES_REQUESTED);
            instance.setCurrentStepKeys(writeJson(List.of()));
            workflowInstanceJpaRepository.save(instance);

            transitionRequestStatus(request, targetStatus, actor.subject(), action.name());
        }

        TaskDecisionEntity saved;
        try {
            saved = taskDecisionJpaRepository.saveAndFlush(decisionEntity);
        } catch (DataIntegrityViolationException conflict) {
            TaskDecisionEntity persisted = taskDecisionJpaRepository
                .findByTaskIdAndIdempotencyKey(taskId, idempotencyKey)
                .orElseThrow(() -> conflict);
            ensureMatchingDecision(persisted, input, actor);
            saved = persisted;
        }

        return toTaskDecisionResource(saved);
    }

    private WorkflowInstanceEntity createInstance(UUID requestId, UUID workflowVersionId) {
        WorkflowInstanceEntity instance = new WorkflowInstanceEntity();
        instance.setId(UUID.randomUUID());
        instance.setRequestId(requestId);
        instance.setWorkflowVersionId(workflowVersionId);
        instance.setStatus(WorkflowInstanceStatus.ACTIVE);
        instance.setCurrentStepKeys(writeJson(List.of()));
        return workflowInstanceJpaRepository.save(instance);
    }

    private void resetInstance(WorkflowInstanceEntity instance, UUID workflowVersionId) {
        List<TaskEntity> existingTasks = taskJpaRepository.findByWorkflowInstanceId(instance.getId());
        if (!existingTasks.isEmpty()) {
            List<UUID> taskIds = existingTasks.stream().map(TaskEntity::getId).toList();
            taskDecisionJpaRepository.deleteByTaskIdIn(taskIds);
            taskJpaRepository.deleteByWorkflowInstanceId(instance.getId());
        }

        instance.setWorkflowVersionId(workflowVersionId);
        instance.setStatus(WorkflowInstanceStatus.ACTIVE);
        instance.setCurrentStepKeys(writeJson(List.of()));
        workflowInstanceJpaRepository.save(instance);
    }

    private RuntimeProgressResult processFromNodes(
        WorkflowInstanceEntity instance,
        RuntimeRequestEntity request,
        WorkflowRuntimeGraph graph,
        Collection<String> startNodeKeys,
        String actorSubject
    ) {
        ArrayDeque<String> queue = new ArrayDeque<>(startNodeKeys);
        Set<String> visitedAutomatic = new HashSet<>();

        while (!queue.isEmpty()) {
            String nodeKey = queue.removeFirst();
            WorkflowNodeInput node = graph.node(nodeKey);

            if (node.type() != WorkflowNodeType.APPROVAL && !visitedAutomatic.add(nodeKey)) {
                throw new IllegalStateException("Automatic runtime traversal detected a loop at node " + nodeKey);
            }

            switch (node.type()) {
                case START -> queue.addAll(graph.successorKeys(nodeKey));
                case APPROVAL -> createApprovalTask(instance, request, nodeKey, node);
                case GATEWAY -> queue.add(resolveGatewayTarget(nodeKey, node, request, graph));
                case JOIN -> {
                    if (isJoinSatisfied(nodeKey, node.join(), instance, graph)) {
                        if (node.join().policy() == WorkflowJoinPolicy.ANY || node.join().policy() == WorkflowJoinPolicy.QUORUM) {
                            skipPendingJoinSiblings(nodeKey, instance, graph);
                        }
                        queue.addAll(graph.successorKeys(nodeKey));
                    }
                }
                case END -> {
                    // Automatic node; final completion is decided once active tasks are inspected below.
                }
                default -> throw new IllegalStateException("Unsupported workflow node type " + node.type());
            }
        }

        Set<String> activeStepKeys = activeStepKeys(instance.getId());
        if (activeStepKeys.isEmpty()) {
            instance.setStatus(WorkflowInstanceStatus.COMPLETED);
            instance.setCurrentStepKeys(writeJson(List.of()));
            workflowInstanceJpaRepository.save(instance);
            return new RuntimeProgressResult(RuntimeRequestStatus.APPROVED);
        }

        instance.setStatus(WorkflowInstanceStatus.ACTIVE);
        instance.setCurrentStepKeys(writeJson(activeStepKeys));
        workflowInstanceJpaRepository.save(instance);
        return new RuntimeProgressResult(null);
    }

    private void createApprovalTask(
        WorkflowInstanceEntity instance,
        RuntimeRequestEntity request,
        String nodeKey,
        WorkflowNodeInput node
    ) {
        boolean hasActiveAtStep = taskJpaRepository
            .findByWorkflowInstanceIdAndStatusIn(instance.getId(), ACTIVE_TASK_STATUSES)
            .stream()
            .anyMatch(existing -> existing.getStepKey().equals(nodeKey));

        if (hasActiveAtStep) {
            return;
        }

        WorkflowAssignmentInput assignment = node.assignment();
        if (assignment == null || assignment.strategy() == null) {
            throw new IllegalStateException("APPROVAL node is missing assignment configuration");
        }

        UUID assigneeUserId = null;
        String assigneeRole = null;

        if (assignment.strategy() == WorkflowAssignmentStrategy.USER) {
            if (!StringUtils.hasText(assignment.userId())) {
                throw new IllegalStateException("USER assignment requires userId");
            }
            try {
                assigneeUserId = UUID.fromString(assignment.userId().trim());
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("USER assignment userId must be a valid UUID");
            }
        } else if (assignment.strategy() == WorkflowAssignmentStrategy.ROLE) {
            if (!StringUtils.hasText(assignment.role())) {
                throw new IllegalStateException("ROLE assignment requires role");
            }
            assigneeRole = assignment.role().trim().toUpperCase(Locale.ROOT);
        } else {
            throw new IllegalStateException("RULE assignment strategy is not supported in E5 runtime");
        }

        TaskEntity task = new TaskEntity();
        task.setId(UUID.randomUUID());
        task.setWorkflowInstanceId(instance.getId());
        task.setRequestId(request.getId());
        task.setStepKey(nodeKey);
        task.setAssigneeUserId(assigneeUserId);
        task.setAssigneeRole(assigneeRole);
        task.setStatus(TaskStatus.PENDING);
        task.setDueAt(resolveDueAt(node));
        task.setClaimedAt(null);
        task.setClaimedByUserId(null);
        task.setJoinPolicy(null);
        task.setQuorumRequired(null);

        taskJpaRepository.save(task);
    }

    private String resolveGatewayTarget(
        String nodeKey,
        WorkflowNodeInput node,
        RuntimeRequestEntity request,
        WorkflowRuntimeGraph graph
    ) {
        WorkflowRuleRefInput ruleRef = node.ruleRef();
        if (ruleRef == null || !StringUtils.hasText(ruleRef.ruleSetKey()) || ruleRef.version() < 1) {
            throw new IllegalStateException("GATEWAY node requires a valid ruleRef");
        }

        RuleEvaluationContext context = new RuleEvaluationContext(
            request.getAmount(),
            request.getDepartment(),
            request.getRequestType(),
            request.getCurrency(),
            readPayload(request.getPayloadJson())
        );

        boolean matched = ruleSetRuntimeEvaluator.matches(ruleRef.ruleSetKey(), ruleRef.version(), context);
        return graph.resolveGatewayTarget(nodeKey, matched);
    }

    private boolean isJoinSatisfied(
        String joinNodeKey,
        WorkflowJoinInput join,
        WorkflowInstanceEntity instance,
        WorkflowRuntimeGraph graph
    ) {
        if (join == null || join.policy() == null) {
            throw new IllegalStateException("JOIN node requires a join policy");
        }

        List<String> predecessorKeys = graph.predecessorKeys(joinNodeKey);
        if (predecessorKeys.isEmpty()) {
            return false;
        }

        List<TaskEntity> predecessorTasks = taskJpaRepository.findByWorkflowInstanceIdAndStepKeyInAndStatusIn(
            instance.getId(),
            predecessorKeys,
            TERMINAL_TASK_STATUSES
        );

        Set<String> approvedStepKeys = predecessorTasks.stream()
            .filter(task -> task.getStatus() == TaskStatus.APPROVED)
            .map(TaskEntity::getStepKey)
            .collect(java.util.stream.Collectors.toSet());

        return switch (join.policy()) {
            case ALL -> predecessorKeys.stream().allMatch(approvedStepKeys::contains);
            case ANY -> !approvedStepKeys.isEmpty();
            case QUORUM -> {
                int quorum = join.quorum() == null ? 0 : join.quorum();
                yield quorum >= 1 && approvedStepKeys.size() >= quorum;
            }
        };
    }

    private void skipPendingJoinSiblings(String joinNodeKey, WorkflowInstanceEntity instance, WorkflowRuntimeGraph graph) {
        List<String> predecessorKeys = graph.predecessorKeys(joinNodeKey);
        if (predecessorKeys.isEmpty()) {
            return;
        }

        List<TaskEntity> siblings = taskJpaRepository.findByWorkflowInstanceIdAndStepKeyInAndStatusIn(
            instance.getId(),
            predecessorKeys,
            ACTIVE_TASK_STATUSES
        );

        for (TaskEntity sibling : siblings) {
            sibling.setStatus(TaskStatus.SKIPPED);
        }

        taskJpaRepository.saveAll(siblings);
    }

    private void cancelSiblingActiveTasks(TaskEntity decidedTask) {
        List<TaskEntity> activeTasks = taskJpaRepository.findByWorkflowInstanceIdAndStatusIn(
            decidedTask.getWorkflowInstanceId(),
            ACTIVE_TASK_STATUSES
        );

        for (TaskEntity activeTask : activeTasks) {
            if (activeTask.getId().equals(decidedTask.getId())) {
                continue;
            }
            activeTask.setStatus(TaskStatus.CANCELLED);
        }

        taskJpaRepository.saveAll(activeTasks);
    }

    private Set<String> activeStepKeys(UUID workflowInstanceId) {
        return taskJpaRepository.findByWorkflowInstanceIdAndStatusIn(workflowInstanceId, ACTIVE_TASK_STATUSES)
            .stream()
            .map(TaskEntity::getStepKey)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private void transitionRequestStatus(
        RuntimeRequestEntity request,
        RuntimeRequestStatus targetStatus,
        String actorSubject,
        String reason
    ) {
        RuntimeRequestStatus sourceStatus = request.getStatus();
        if (sourceStatus == targetStatus) {
            return;
        }

        request.setStatus(targetStatus);
        runtimeRequestJpaRepository.save(request);

        runtimeRequestStatusTransitionJpaRepository.save(new RuntimeRequestStatusTransitionEntity(
            UUID.randomUUID(),
            request.getId(),
            sourceStatus.name(),
            targetStatus.name(),
            actorSubject,
            reason,
            Instant.now()
        ));
    }

    private void authorizeTaskAction(TaskEntity task, RuntimeRequestEntity request, TaskActor actor) {
        AccessDecision decision = accessPolicyService.canDecideTask(new TaskAccessContext(
            actor.userId(),
            actor.roles(),
            task.getAssigneeUserId(),
            task.getAssigneeRole(),
            actor.department(),
            request.getDepartment()
        ));

        if (!decision.allowed()) {
            throw new AccessDeniedException("Task access denied: " + decision.reasonCode());
        }
    }

    private void ensureDecisionCommentRequired(TaskDecisionAction action, String comment) {
        if ((action == TaskDecisionAction.REJECT || action == TaskDecisionAction.SEND_BACK) && !StringUtils.hasText(comment)) {
            throw new BadRequestException(
                "Comment is required for REJECT and SEND_BACK",
                List.of(new ApiErrorDetail("comment", "must be provided for REJECT or SEND_BACK"))
            );
        }
    }

    private void ensureMatchingDecision(TaskDecisionEntity existing, TaskDecisionInput input, TaskActor actor) {
        String inputComment = trimToNull(input.comment());
        String existingComment = trimToNull(existing.getComment());

        boolean matches = existing.getAction() == input.action()
            && Objects.equals(existingComment, inputComment)
            && Objects.equals(existing.getActedByUserId(), actor.userId());

        if (!matches) {
            throw new IllegalStateException("Idempotency key has already been used for another task decision");
        }
    }

    private void ensureMatchingRequestHash(RuntimeIdempotencyKeyEntity existing, String requestHash) {
        if (!Objects.equals(existing.getRequestHash(), requestHash)) {
            throw new IllegalStateException("Idempotency key has already been used for another request");
        }
    }

    private Specification<TaskEntity> assignmentSpecification(TaskActor actor, TaskAssignedToFilter assignedTo) {
        Specification<TaskEntity> byMe = (root, query, builder) -> builder.or(
            builder.equal(root.get("assigneeUserId"), actor.userId()),
            builder.equal(root.get("claimedByUserId"), actor.userId())
        );

        Specification<TaskEntity> byRole = (root, query, builder) -> {
            if (actor.roles().isEmpty()) {
                return builder.disjunction();
            }

            var rolePredicate = root.get("assigneeRole").in(actor.roles());
            var unclaimedOrMine = builder.or(
                builder.isNull(root.get("claimedByUserId")),
                builder.equal(root.get("claimedByUserId"), actor.userId())
            );
            return builder.and(rolePredicate, unclaimedOrMine);
        };

        if (assignedTo == TaskAssignedToFilter.me) {
            return byMe;
        }

        if (assignedTo == TaskAssignedToFilter.role) {
            return byRole;
        }

        return byMe.or(byRole);
    }

    private Sort parseSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] tokens = sort.split(",", 2);
        String requestedField = tokens[0].trim();
        String mappedField = SORT_FIELD_MAPPING.getOrDefault(requestedField, "createdAt");
        Sort.Direction direction = Sort.Direction.DESC;

        if (tokens.length > 1 && tokens[1].equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        }

        return Sort.by(direction, mappedField);
    }

    private TaskResource toTaskResource(TaskEntity task) {
        return new TaskResource(
            task.getId(),
            task.getRequestId(),
            task.getWorkflowInstanceId(),
            task.getStepKey(),
            task.getAssigneeUserId(),
            task.getAssigneeRole(),
            task.getStatus(),
            task.getDueAt(),
            task.getClaimedAt(),
            task.getClaimedByUserId(),
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }

    private TaskDecisionResource toTaskDecisionResource(TaskDecisionEntity decision) {
        return new TaskDecisionResource(
            decision.getId(),
            decision.getTaskId(),
            decision.getAction(),
            decision.getComment(),
            decision.getActedByUserId(),
            decision.getActedOnBehalfOfUserId(),
            decision.getCreatedAt()
        );
    }

    private Instant resolveDueAt(WorkflowNodeInput node) {
        if (node.sla() == null || node.sla().dueInHours() == null) {
            return null;
        }

        return Instant.now().plusSeconds((long) node.sla().dueInHours() * 3600);
    }

    private Map<String, Object> readPayload(String payloadJson) {
        try {
            if (!StringUtils.hasText(payloadJson)) {
                return Map.of();
            }
            return objectMapper.readValue(payloadJson, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize request payload JSON", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize runtime JSON", exception);
        }
    }

    private String computeRequestHash(String scope, UUID taskId, UUID userId) {
        byte[] bytes = (scope + ":" + taskId + ":" + userId).getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public record RuntimeBootstrapResult(RuntimeRequestStatus requestStatus) {
    }

    private record RuntimeProgressResult(RuntimeRequestStatus terminalRequestStatus) {
    }
}
