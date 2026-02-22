package com.isaac.approvalworkflowengine.workflowtemplate.validation;

import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowAssignmentInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowEdgeInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowGraphInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowJoinInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowNodeInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowRuleRefInput;
import com.isaac.approvalworkflowengine.workflowtemplate.model.WorkflowAssignmentStrategy;
import com.isaac.approvalworkflowengine.workflowtemplate.model.WorkflowJoinPolicy;
import com.isaac.approvalworkflowengine.workflowtemplate.model.WorkflowNodeType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WorkflowGraphValidator {

    public void validateForActivation(WorkflowGraphInput graph, boolean allowLoopback) {
        if (graph == null) {
            throw new IllegalStateException("Workflow graph is required for activation");
        }

        List<WorkflowNodeInput> nodes = graph.nodes() == null ? List.of() : graph.nodes();
        List<WorkflowEdgeInput> edges = graph.edges() == null ? List.of() : graph.edges();
        if (nodes.isEmpty() || edges.isEmpty()) {
            throw new IllegalStateException("Workflow graph must contain nodes and edges");
        }

        Map<String, WorkflowNodeInput> nodesById = new HashMap<>();
        Map<String, List<String>> outgoing = new HashMap<>();
        Map<String, List<String>> incoming = new HashMap<>();

        int startCount = 0;
        int endCount = 0;
        String startNodeId = null;
        String endNodeId = null;

        for (WorkflowNodeInput node : nodes) {
            String nodeId = normalizeKey(node.id());
            if (nodesById.putIfAbsent(nodeId, node) != null) {
                throw new IllegalStateException("Duplicate node id is not allowed: " + nodeId);
            }

            outgoing.put(nodeId, new ArrayList<>());
            incoming.put(nodeId, new ArrayList<>());

            if (node.type() == WorkflowNodeType.START) {
                startCount++;
                startNodeId = nodeId;
            }
            if (node.type() == WorkflowNodeType.END) {
                endCount++;
                endNodeId = nodeId;
            }
        }

        if (startCount != 1 || endCount != 1) {
            throw new IllegalStateException("Workflow graph must include exactly one START and one END node");
        }

        for (WorkflowEdgeInput edge : edges) {
            String fromNode = normalizeKey(edge.from());
            String toNode = normalizeKey(edge.to());

            if (!nodesById.containsKey(fromNode) || !nodesById.containsKey(toNode)) {
                throw new IllegalStateException("Workflow graph contains dangling edge references");
            }

            outgoing.get(fromNode).add(toNode);
            incoming.get(toNode).add(fromNode);
        }

        if (!incoming.get(startNodeId).isEmpty()) {
            throw new IllegalStateException("START node cannot have incoming edges");
        }

        if (!outgoing.get(endNodeId).isEmpty()) {
            throw new IllegalStateException("END node cannot have outgoing edges");
        }

        for (Map.Entry<String, WorkflowNodeInput> entry : nodesById.entrySet()) {
            String nodeId = entry.getKey();
            WorkflowNodeInput node = entry.getValue();

            if (node.type() != WorkflowNodeType.END && outgoing.get(nodeId).isEmpty()) {
                throw new IllegalStateException("Non-terminal nodes must have at least one outgoing edge");
            }

            validateNodeConfiguration(node, incoming.get(nodeId).size());
        }

        Set<String> reachableFromStart = traverse(startNodeId, outgoing);
        if (reachableFromStart.size() != nodesById.size()) {
            throw new IllegalStateException("All nodes must be reachable from START");
        }

        Set<String> canReachEnd = traverse(endNodeId, incoming);
        if (canReachEnd.size() != nodesById.size()) {
            throw new IllegalStateException("All nodes must be able to reach END");
        }

        if (!allowLoopback && hasCycle(nodesById.keySet(), outgoing)) {
            throw new IllegalStateException("Workflow graph contains a cycle but loopback is disabled");
        }
    }

    private void validateNodeConfiguration(WorkflowNodeInput node, int incomingEdges) {
        if (node.type() == WorkflowNodeType.APPROVAL) {
            validateAssignment(node.assignment());
        }

        if (node.type() == WorkflowNodeType.GATEWAY) {
            validateRuleRef(node.ruleRef());
        }

        if (node.type() == WorkflowNodeType.JOIN) {
            validateJoin(node.join(), incomingEdges);
        }
    }

    private void validateAssignment(WorkflowAssignmentInput assignment) {
        if (assignment == null || assignment.strategy() == null) {
            throw new IllegalStateException("APPROVAL nodes require a valid assignment strategy");
        }

        if (assignment.strategy() == WorkflowAssignmentStrategy.ROLE && !StringUtils.hasText(assignment.role())) {
            throw new IllegalStateException("ROLE assignment requires role value");
        }

        if (assignment.strategy() == WorkflowAssignmentStrategy.USER && !StringUtils.hasText(assignment.userId())) {
            throw new IllegalStateException("USER assignment requires userId value");
        }

        if (assignment.strategy() == WorkflowAssignmentStrategy.RULE && !StringUtils.hasText(assignment.expression())) {
            throw new IllegalStateException("RULE assignment requires expression value");
        }
    }

    private void validateRuleRef(WorkflowRuleRefInput ruleRef) {
        if (ruleRef == null || !StringUtils.hasText(ruleRef.ruleSetKey()) || ruleRef.version() < 1) {
            throw new IllegalStateException("GATEWAY nodes require a valid ruleRef (ruleSetKey + version >= 1)");
        }
    }

    private void validateJoin(WorkflowJoinInput join, int incomingEdges) {
        if (join == null || join.policy() == null) {
            throw new IllegalStateException("JOIN nodes require an explicit join policy");
        }

        if (join.policy() == WorkflowJoinPolicy.QUORUM) {
            Integer quorum = join.quorum();
            if (quorum == null || quorum < 1 || quorum > Math.max(incomingEdges, 1)) {
                throw new IllegalStateException("JOIN quorum must be between 1 and incoming edge count");
            }
        }
    }

    private Set<String> traverse(String start, Map<String, List<String>> graph) {
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            String nodeId = queue.removeFirst();
            if (!visited.add(nodeId)) {
                continue;
            }

            for (String nextNode : graph.getOrDefault(nodeId, List.of())) {
                queue.addLast(nextNode);
            }
        }

        return visited;
    }

    private boolean hasCycle(Set<String> nodeIds, Map<String, List<String>> outgoing) {
        Map<String, Integer> visitState = new HashMap<>();

        for (String nodeId : nodeIds) {
            if (detectCycle(nodeId, outgoing, visitState)) {
                return true;
            }
        }

        return false;
    }

    private boolean detectCycle(String nodeId, Map<String, List<String>> outgoing, Map<String, Integer> visitState) {
        Integer state = visitState.get(nodeId);
        if (state != null) {
            if (state == 1) {
                return true;
            }
            if (state == 2) {
                return false;
            }
        }

        visitState.put(nodeId, 1);
        for (String child : outgoing.getOrDefault(nodeId, List.of())) {
            if (detectCycle(child, outgoing, visitState)) {
                return true;
            }
        }

        visitState.put(nodeId, 2);
        return false;
    }

    private String normalizeKey(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Node and edge ids must be non-empty");
        }
        return value.trim();
    }
}
