package com.isaac.approvalworkflowengine.workflowtemplate.checksum;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowAssignmentInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowEdgeInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowGraphInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowJoinInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowNodeInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowRuleRefInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowSlaInput;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

@Component
public class WorkflowGraphChecksumService {

    private final ObjectMapper canonicalMapper;

    public WorkflowGraphChecksumService(ObjectMapper objectMapper) {
        this.canonicalMapper = objectMapper.copy()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public String canonicalize(WorkflowGraphInput graph) {
        WorkflowGraphInput canonical = canonicalGraph(graph);
        try {
            return canonicalMapper.writeValueAsString(canonical);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize workflow graph", exception);
        }
    }

    public String checksumSha256(String canonicalGraphJson) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = canonicalGraphJson.getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }

    private WorkflowGraphInput canonicalGraph(WorkflowGraphInput graph) {
        List<WorkflowNodeInput> nodes = graph.nodes() == null ? List.of() : graph.nodes();
        List<WorkflowEdgeInput> edges = graph.edges() == null ? List.of() : graph.edges();

        List<WorkflowNodeInput> canonicalNodes = new ArrayList<>(nodes.stream()
            .map(this::canonicalNode)
            .sorted(Comparator.comparing(WorkflowNodeInput::id))
            .toList());

        List<WorkflowEdgeInput> canonicalEdges = new ArrayList<>(edges.stream()
            .map(this::canonicalEdge)
            .sorted((left, right) -> {
                int fromComparison = left.from().compareTo(right.from());
                if (fromComparison != 0) {
                    return fromComparison;
                }
                int toComparison = left.to().compareTo(right.to());
                if (toComparison != 0) {
                    return toComparison;
                }
                return canonicalConditionString(left.condition())
                    .compareTo(canonicalConditionString(right.condition()));
            })
            .toList());

        Map<String, Object> canonicalPolicies = canonicalMap(graph.policies());
        return new WorkflowGraphInput(canonicalNodes, canonicalEdges, canonicalPolicies);
    }

    private WorkflowNodeInput canonicalNode(WorkflowNodeInput node) {
        WorkflowAssignmentInput assignment = node.assignment() == null
            ? null
            : new WorkflowAssignmentInput(
                node.assignment().strategy(),
                node.assignment().role(),
                node.assignment().userId(),
                node.assignment().expression()
            );

        WorkflowRuleRefInput ruleRef = node.ruleRef() == null
            ? null
            : new WorkflowRuleRefInput(node.ruleRef().ruleSetKey(), node.ruleRef().version());

        WorkflowJoinInput join = node.join() == null
            ? null
            : new WorkflowJoinInput(node.join().policy(), node.join().quorum());

        WorkflowSlaInput sla = node.sla() == null
            ? null
            : new WorkflowSlaInput(node.sla().dueInHours());

        return new WorkflowNodeInput(
            node.id().trim(),
            node.type(),
            assignment,
            ruleRef,
            join,
            sla
        );
    }

    private WorkflowEdgeInput canonicalEdge(WorkflowEdgeInput edge) {
        return new WorkflowEdgeInput(
            edge.from().trim(),
            edge.to().trim(),
            canonicalMap(edge.condition())
        );
    }

    private String canonicalConditionString(Map<String, Object> condition) {
        if (condition == null || condition.isEmpty()) {
            return "{}";
        }

        try {
            return canonicalMapper.writeValueAsString(canonicalMap(condition));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize edge condition", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> canonicalMap(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> sorted = new TreeMap<>();
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            Object normalized = normalizeValue(entry.getValue());
            if (normalized != null) {
                sorted.put(entry.getKey(), normalized);
            }
        }

        Map<String, Object> ordered = new LinkedHashMap<>();
        ordered.putAll(sorted);
        return ordered;
    }

    @SuppressWarnings("unchecked")
    private Object normalizeValue(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> normalized = new TreeMap<>();
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                Object key = entry.getKey();
                if (key != null) {
                    Object normalizedValue = normalizeValue(entry.getValue());
                    if (normalizedValue != null) {
                        normalized.put(key.toString(), normalizedValue);
                    }
                }
            }
            return new LinkedHashMap<>(normalized);
        }

        if (value instanceof List<?> listValue) {
            List<Object> normalized = new ArrayList<>();
            for (Object item : listValue) {
                normalized.add(normalizeValue(item));
            }
            return normalized;
        }

        return value;
    }
}
