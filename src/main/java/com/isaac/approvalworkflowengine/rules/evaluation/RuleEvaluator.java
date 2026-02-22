package com.isaac.approvalworkflowengine.rules.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isaac.approvalworkflowengine.rules.model.AllExpression;
import com.isaac.approvalworkflowengine.rules.model.AnyExpression;
import com.isaac.approvalworkflowengine.rules.model.NotExpression;
import com.isaac.approvalworkflowengine.rules.model.PredicateExpression;
import com.isaac.approvalworkflowengine.rules.model.RuleEvaluationContext;
import com.isaac.approvalworkflowengine.rules.model.RuleExpression;
import com.isaac.approvalworkflowengine.rules.model.RuleOperator;
import com.isaac.approvalworkflowengine.rules.validation.RuleRegexGuard;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class RuleEvaluator {

    private final ObjectMapper objectMapper;
    private final RuleFieldResolver ruleFieldResolver;
    private final RuleRegexGuard ruleRegexGuard;

    public RuleEvaluator(
        ObjectMapper objectMapper,
        RuleFieldResolver ruleFieldResolver,
        RuleRegexGuard ruleRegexGuard
    ) {
        this.objectMapper = objectMapper;
        this.ruleFieldResolver = ruleFieldResolver;
        this.ruleRegexGuard = ruleRegexGuard;
    }

    public RuleEvaluationResult evaluate(RuleExpression expression, RuleEvaluationContext context) {
        List<RuleEvaluationTrace> traces = new ArrayList<>();
        Map<String, Pattern> regexCache = new HashMap<>();
        boolean matched = evaluateInternal(expression, context, "$", traces, regexCache);
        return new RuleEvaluationResult(matched, List.copyOf(traces));
    }

    private boolean evaluateInternal(
        RuleExpression expression,
        RuleEvaluationContext context,
        String path,
        List<RuleEvaluationTrace> traces,
        Map<String, Pattern> regexCache
    ) {
        if (expression instanceof AllExpression allExpression) {
            boolean result = true;
            List<RuleExpression> children = allExpression.expressions();
            for (int index = 0; index < children.size(); index++) {
                boolean child = evaluateInternal(children.get(index), context, path + ".all[" + index + "]", traces, regexCache);
                result = result && child;
            }
            traces.add(new RuleEvaluationTrace(path, "all", result, null, null, null, null, "all children evaluated"));
            return result;
        }

        if (expression instanceof AnyExpression anyExpression) {
            boolean result = false;
            List<RuleExpression> children = anyExpression.expressions();
            for (int index = 0; index < children.size(); index++) {
                boolean child = evaluateInternal(children.get(index), context, path + ".any[" + index + "]", traces, regexCache);
                result = result || child;
            }
            traces.add(new RuleEvaluationTrace(path, "any", result, null, null, null, null, "all children evaluated"));
            return result;
        }

        if (expression instanceof NotExpression notExpression) {
            boolean child = evaluateInternal(notExpression.expression(), context, path + ".not", traces, regexCache);
            boolean result = !child;
            traces.add(new RuleEvaluationTrace(path, "not", result, null, null, null, null, "logical negation"));
            return result;
        }

        PredicateExpression predicate = (PredicateExpression) expression;
        return evaluatePredicate(predicate, context, path, traces, regexCache);
    }

    private boolean evaluatePredicate(
        PredicateExpression predicate,
        RuleEvaluationContext context,
        String path,
        List<RuleEvaluationTrace> traces,
        Map<String, Pattern> regexCache
    ) {
        Object fieldValue = ruleFieldResolver.resolve(predicate.field(), context);
        JsonNode expectedNode = predicate.value();
        Object expectedValue = jsonNodeToObject(expectedNode);

        boolean result;
        String reason;

        switch (predicate.operator()) {
            case EQ -> {
                result = equalsNormalized(fieldValue, expectedValue);
                reason = "strict equality";
            }
            case NE -> {
                result = !equalsNormalized(fieldValue, expectedValue);
                reason = "strict inequality";
            }
            case GT -> {
                result = compareNumbers(fieldValue, expectedValue, comparison -> comparison > 0);
                reason = "numeric greater-than";
            }
            case GTE -> {
                result = compareNumbers(fieldValue, expectedValue, comparison -> comparison >= 0);
                reason = "numeric greater-than-or-equal";
            }
            case LT -> {
                result = compareNumbers(fieldValue, expectedValue, comparison -> comparison < 0);
                reason = "numeric less-than";
            }
            case LTE -> {
                result = compareNumbers(fieldValue, expectedValue, comparison -> comparison <= 0);
                reason = "numeric less-than-or-equal";
            }
            case IN -> {
                result = isIn(fieldValue, expectedNode);
                reason = "membership check";
            }
            case CONTAINS -> {
                result = contains(fieldValue, expectedValue);
                reason = "contains check";
            }
            case MATCHES -> {
                result = matchesRegex(fieldValue, expectedNode, regexCache, path + ".value");
                reason = "regex match";
            }
            default -> {
                result = false;
                reason = "unsupported operator";
            }
        }

        traces.add(new RuleEvaluationTrace(
            path,
            "predicate",
            result,
            predicate.field(),
            predicate.operator().token(),
            fieldValue,
            expectedValue,
            reason
        ));

        return result;
    }

    private boolean compareNumbers(Object left, Object right, IntPredicate comparisonPredicate) {
        BigDecimal leftNumeric = asBigDecimal(left);
        BigDecimal rightNumeric = asBigDecimal(right);

        if (leftNumeric == null || rightNumeric == null) {
            return false;
        }

        return comparisonPredicate.test(leftNumeric.compareTo(rightNumeric));
    }

    private boolean isIn(Object fieldValue, JsonNode expectedNode) {
        if (expectedNode == null || !expectedNode.isArray()) {
            return false;
        }

        for (JsonNode item : expectedNode) {
            if (equalsNormalized(fieldValue, jsonNodeToObject(item))) {
                return true;
            }
        }

        return false;
    }

    private boolean contains(Object fieldValue, Object expectedValue) {
        if (fieldValue instanceof String leftString) {
            return expectedValue != null && leftString.contains(String.valueOf(expectedValue));
        }

        if (fieldValue instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (equalsNormalized(item, expectedValue)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean matchesRegex(
        Object fieldValue,
        JsonNode expectedNode,
        Map<String, Pattern> regexCache,
        String patternPath
    ) {
        if (!(fieldValue instanceof String inputValue) || expectedNode == null || !expectedNode.isTextual()) {
            return false;
        }

        String patternValue = expectedNode.asText();
        Pattern pattern = regexCache.computeIfAbsent(patternValue, value -> ruleRegexGuard.compile(value, patternPath));
        ruleRegexGuard.validateInput(inputValue, patternPath);
        return pattern.matcher(inputValue).matches();
    }

    private boolean equalsNormalized(Object left, Object right) {
        BigDecimal leftNumeric = asBigDecimal(left);
        BigDecimal rightNumeric = asBigDecimal(right);

        if (leftNumeric != null && rightNumeric != null) {
            return leftNumeric.compareTo(rightNumeric) == 0;
        }

        return Objects.equals(left, right);
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal decimal) {
            return decimal;
        }

        if (value instanceof Number number) {
            try {
                return new BigDecimal(number.toString());
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        return null;
    }

    private Object jsonNodeToObject(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        return objectMapper.convertValue(value, Object.class);
    }
}
