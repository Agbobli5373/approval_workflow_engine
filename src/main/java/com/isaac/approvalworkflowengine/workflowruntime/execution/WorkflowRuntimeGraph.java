package com.isaac.approvalworkflowengine.workflowruntime.execution;

import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowEdgeInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowGraphInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowNodeInput;
import com.isaac.approvalworkflowengine.workflowtemplate.model.WorkflowNodeType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class WorkflowRuntimeGraph {

    private final Map<String, WorkflowNodeInput> nodesById;
    private final Map<String, List<RuntimeEdge>> outgoing;
    private final Map<String, List<RuntimeEdge>> incoming;

    private WorkflowRuntimeGraph(
        Map<String, WorkflowNodeInput> nodesById,
        Map<String, List<RuntimeEdge>> outgoing,
        Map<String, List<RuntimeEdge>> incoming
    ) {
        this.nodesById = nodesById;
        this.outgoing = outgoing;
        this.incoming = incoming;
    }

    public static WorkflowRuntimeGraph from(WorkflowGraphInput graph) {
        Map<String, WorkflowNodeInput> nodesById = new HashMap<>();
        Map<String, List<RuntimeEdge>> outgoing = new HashMap<>();
        Map<String, List<RuntimeEdge>> incoming = new HashMap<>();

        for (WorkflowNodeInput node : graph.nodes()) {
            String nodeId = normalizeKey(node.id());
            nodesById.put(nodeId, node);
            outgoing.put(nodeId, new ArrayList<>());
            incoming.put(nodeId, new ArrayList<>());
        }

        for (WorkflowEdgeInput edge : graph.edges()) {
            String from = normalizeKey(edge.from());
            String to = normalizeKey(edge.to());
            RuntimeEdge runtimeEdge = new RuntimeEdge(from, to, readBranch(edge.condition()));
            outgoing.computeIfAbsent(from, key -> new ArrayList<>()).add(runtimeEdge);
            incoming.computeIfAbsent(to, key -> new ArrayList<>()).add(runtimeEdge);
        }

        return new WorkflowRuntimeGraph(nodesById, outgoing, incoming);
    }

    public WorkflowNodeInput startNode() {
        return nodesById.values().stream()
            .filter(node -> node.type() == WorkflowNodeType.START)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Workflow graph missing START node"));
    }

    public WorkflowNodeInput node(String nodeKey) {
        WorkflowNodeInput node = nodesById.get(nodeKey);
        if (node == null) {
            throw new IllegalStateException("Workflow graph missing node " + nodeKey);
        }
        return node;
    }

    public List<String> successorKeys(String nodeKey) {
        return outgoing.getOrDefault(nodeKey, List.of()).stream()
            .map(RuntimeEdge::to)
            .toList();
    }

    public List<String> predecessorKeys(String nodeKey) {
        return incoming.getOrDefault(nodeKey, List.of()).stream()
            .map(RuntimeEdge::from)
            .toList();
    }

    public String resolveGatewayTarget(String gatewayNodeKey, boolean branchOutcome) {
        List<RuntimeEdge> edges = outgoing.getOrDefault(gatewayNodeKey, List.of());
        RuntimeEdge matched = edges.stream()
            .filter(edge -> edge.branch() != null && edge.branch() == branchOutcome)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Gateway node " + gatewayNodeKey + " has no branch for outcome " + branchOutcome
            ));

        return matched.to();
    }

    public List<RuntimeEdge> outgoingEdges(String nodeKey) {
        return List.copyOf(outgoing.getOrDefault(nodeKey, List.of()));
    }

    public Collection<WorkflowNodeInput> nodes() {
        return List.copyOf(nodesById.values());
    }

    private static String normalizeKey(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Workflow node/edge key cannot be blank");
        }
        return value.trim();
    }

    private static Boolean readBranch(Map<String, Object> condition) {
        if (condition == null || condition.isEmpty()) {
            return null;
        }

        Object value = condition.get("branch");
        if (value == null) {
            return null;
        }

        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }

        if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
            String normalized = stringValue.trim().toLowerCase(Locale.ROOT);
            if ("true".equals(normalized)) {
                return true;
            }
            if ("false".equals(normalized)) {
                return false;
            }
        }

        throw new IllegalStateException("Gateway branch condition must be boolean");
    }

    public record RuntimeEdge(String from, String to, Boolean branch) {
    }
}
