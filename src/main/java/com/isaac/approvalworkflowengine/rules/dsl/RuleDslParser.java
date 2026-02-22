package com.isaac.approvalworkflowengine.rules.dsl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.isaac.approvalworkflowengine.rules.model.AllExpression;
import com.isaac.approvalworkflowengine.rules.model.AnyExpression;
import com.isaac.approvalworkflowengine.rules.model.NotExpression;
import com.isaac.approvalworkflowengine.rules.model.PredicateExpression;
import com.isaac.approvalworkflowengine.rules.model.RuleExpression;
import com.isaac.approvalworkflowengine.rules.model.RuleOperator;
import com.isaac.approvalworkflowengine.rules.validation.RuleRegexGuard;
import com.isaac.approvalworkflowengine.shared.api.ApiErrorDetail;
import com.isaac.approvalworkflowengine.shared.error.BadRequestException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class RuleDslParser {

    private static final Pattern FIELD_PATTERN = Pattern.compile(
        "^(amount|department|requestType|currency|payload(?:\\.[A-Za-z0-9_-]+)*)$"
    );

    private static final Set<String> ALL_NODE_KEYS = Set.of("all");
    private static final Set<String> ANY_NODE_KEYS = Set.of("any");
    private static final Set<String> NOT_NODE_KEYS = Set.of("not");
    private static final Set<String> PREDICATE_KEYS = Set.of("field", "op", "value");

    private final RuleRegexGuard ruleRegexGuard;

    public RuleDslParser(RuleRegexGuard ruleRegexGuard) {
        this.ruleRegexGuard = ruleRegexGuard;
    }

    public RuleExpression parse(JsonNode dsl) {
        return parse(dsl, "dsl");
    }

    private RuleExpression parse(JsonNode node, String path) {
        if (node == null || node.isNull() || !node.isObject()) {
            throw invalid(path, "Expression must be a JSON object");
        }

        ObjectNode objectNode = (ObjectNode) node;
        Set<String> keys = extractKeys(objectNode);

        boolean hasAll = keys.contains("all");
        boolean hasAny = keys.contains("any");
        boolean hasNot = keys.contains("not");
        boolean hasPredicateKeys = keys.contains("field") || keys.contains("op") || keys.contains("value");

        int nodeShapeCount = (hasAll ? 1 : 0) + (hasAny ? 1 : 0) + (hasNot ? 1 : 0) + (hasPredicateKeys ? 1 : 0);
        if (nodeShapeCount != 1) {
            throw invalid(path, "Expression must have exactly one shape: all, any, not, or predicate");
        }

        if (hasAll) {
            ensureExactKeys(keys, ALL_NODE_KEYS, path);
            JsonNode expressions = objectNode.get("all");
            if (expressions == null || !expressions.isArray() || expressions.isEmpty()) {
                throw invalid(path + ".all", "all must be a non-empty array");
            }

            List<RuleExpression> parsed = new ArrayList<>();
            for (int index = 0; index < expressions.size(); index++) {
                parsed.add(parse(expressions.get(index), path + ".all[" + index + "]"));
            }
            return new AllExpression(List.copyOf(parsed));
        }

        if (hasAny) {
            ensureExactKeys(keys, ANY_NODE_KEYS, path);
            JsonNode expressions = objectNode.get("any");
            if (expressions == null || !expressions.isArray() || expressions.isEmpty()) {
                throw invalid(path + ".any", "any must be a non-empty array");
            }

            List<RuleExpression> parsed = new ArrayList<>();
            for (int index = 0; index < expressions.size(); index++) {
                parsed.add(parse(expressions.get(index), path + ".any[" + index + "]"));
            }
            return new AnyExpression(List.copyOf(parsed));
        }

        if (hasNot) {
            ensureExactKeys(keys, NOT_NODE_KEYS, path);
            JsonNode expression = objectNode.get("not");
            return new NotExpression(parse(expression, path + ".not"));
        }

        ensureExactKeys(keys, PREDICATE_KEYS, path);

        JsonNode fieldNode = objectNode.get("field");
        if (fieldNode == null || !fieldNode.isTextual() || fieldNode.asText().isBlank()) {
            throw invalid(path + ".field", "field must be a non-blank string");
        }

        String field = fieldNode.asText().trim();
        if (!FIELD_PATTERN.matcher(field).matches()) {
            throw invalid(path + ".field", "field path is not allowed");
        }

        JsonNode operatorNode = objectNode.get("op");
        if (operatorNode == null || !operatorNode.isTextual()) {
            throw invalid(path + ".op", "op must be a string");
        }

        RuleOperator operator = RuleOperator.fromToken(operatorNode.asText());
        if (operator == null) {
            throw invalid(path + ".op", "op is not supported");
        }

        JsonNode valueNode = objectNode.get("value");
        if (valueNode == null) {
            throw invalid(path + ".value", "value is required");
        }

        if (operator == RuleOperator.IN) {
            if (!valueNode.isArray() || valueNode.isEmpty()) {
                throw invalid(path + ".value", "in operator requires a non-empty array value");
            }
        }

        if (operator == RuleOperator.MATCHES) {
            if (!valueNode.isTextual()) {
                throw invalid(path + ".value", "matches operator requires a string pattern");
            }
            ruleRegexGuard.validatePattern(valueNode.asText(), path + ".value");
        }

        return new PredicateExpression(field, operator, valueNode.deepCopy());
    }

    private void ensureExactKeys(Set<String> actualKeys, Set<String> allowedKeys, String path) {
        if (!actualKeys.equals(allowedKeys)) {
            throw invalid(path, "Expression contains unexpected keys");
        }
    }

    private Set<String> extractKeys(ObjectNode node) {
        Set<String> keys = new HashSet<>();
        node.fieldNames().forEachRemaining(keys::add);
        return keys;
    }

    private BadRequestException invalid(String field, String reason) {
        return new BadRequestException(
            "Rule DSL is invalid",
            List.of(new ApiErrorDetail(field, reason))
        );
    }
}
