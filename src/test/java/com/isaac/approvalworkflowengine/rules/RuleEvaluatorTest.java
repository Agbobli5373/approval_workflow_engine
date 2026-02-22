package com.isaac.approvalworkflowengine.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isaac.approvalworkflowengine.rules.dsl.RuleDslParser;
import com.isaac.approvalworkflowengine.rules.evaluation.RuleEvaluationResult;
import com.isaac.approvalworkflowengine.rules.evaluation.RuleEvaluator;
import com.isaac.approvalworkflowengine.rules.evaluation.RuleFieldResolver;
import com.isaac.approvalworkflowengine.rules.model.RuleEvaluationContext;
import com.isaac.approvalworkflowengine.rules.model.RuleExpression;
import com.isaac.approvalworkflowengine.rules.validation.RuleRegexGuard;
import com.isaac.approvalworkflowengine.shared.error.BadRequestException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuleEvaluatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RuleRegexGuard regexGuard = new RuleRegexGuard();
    private final RuleDslParser parser = new RuleDslParser(regexGuard);
    private final RuleEvaluator evaluator = new RuleEvaluator(objectMapper, new RuleFieldResolver(), regexGuard);

    @Test
    void evaluatesAllOperatorsCorrectly() throws Exception {
        RuleEvaluationContext context = defaultContext();

        assertThat(evaluate("{\"field\":\"department\",\"op\":\"==\",\"value\":\"Finance\"}", context).matched()).isTrue();
        assertThat(evaluate("{\"field\":\"requestType\",\"op\":\"!=\",\"value\":\"PROCUREMENT\"}", context).matched()).isTrue();
        assertThat(evaluate("{\"field\":\"amount\",\"op\":\">\",\"value\":1000}", context).matched()).isTrue();
        assertThat(evaluate("{\"field\":\"amount\",\"op\":\">=\",\"value\":1500}", context).matched()).isTrue();
        assertThat(evaluate("{\"field\":\"amount\",\"op\":\"<\",\"value\":2000}", context).matched()).isTrue();
        assertThat(evaluate("{\"field\":\"amount\",\"op\":\"<=\",\"value\":1500}", context).matched()).isTrue();
        assertThat(evaluate("{\"field\":\"department\",\"op\":\"in\",\"value\":[\"Finance\",\"HR\"]}", context).matched()).isTrue();
        assertThat(evaluate("{\"field\":\"payload.vendor\",\"op\":\"contains\",\"value\":\"Acme\"}", context).matched()).isTrue();
        assertThat(evaluate("{\"field\":\"payload.tags\",\"op\":\"contains\",\"value\":\"urgent\"}", context).matched()).isTrue();
        assertThat(evaluate("{\"field\":\"payload.code\",\"op\":\"matches\",\"value\":\"EXP-[0-9]{4}\"}", context).matched()).isTrue();
    }

    @Test
    void missingFieldEvaluatesDeterministicallyToFalseForNumericComparison() throws Exception {
        RuleEvaluationResult result = evaluate(
            "{\"field\":\"payload.missing\",\"op\":\">\",\"value\":1}",
            defaultContext()
        );

        assertThat(result.matched()).isFalse();
    }

    @Test
    void evaluationIsDeterministicForSameInput() throws Exception {
        RuleEvaluationContext context = defaultContext();
        String dsl = """
            {
              "all": [
                {"field": "amount", "op": ">=", "value": 1000},
                {"field": "department", "op": "==", "value": "Finance"},
                {"field": "payload.code", "op": "matches", "value": "EXP-[0-9]{4}"}
              ]
            }
            """;

        RuleEvaluationResult first = evaluate(dsl, context);
        RuleEvaluationResult second = evaluate(dsl, context);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void regexInputGuardRejectsOversizedInput() throws Exception {
        String longCode = "X".repeat(4_001);
        RuleEvaluationContext context = new RuleEvaluationContext(
            BigDecimal.valueOf(1500),
            "Finance",
            "EXPENSE",
            "USD",
            Map.of("code", longCode)
        );

        RuleExpression expression = parser.parse(objectMapper.readTree(
            "{\"field\":\"payload.code\",\"op\":\"matches\",\"value\":\"X+\"}"
        ));

        assertThatThrownBy(() -> evaluator.evaluate(expression, context))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Rule DSL is invalid");
    }

    private RuleEvaluationResult evaluate(String dsl, RuleEvaluationContext context) throws Exception {
        RuleExpression expression = parser.parse(objectMapper.readTree(dsl));
        return evaluator.evaluate(expression, context);
    }

    private RuleEvaluationContext defaultContext() {
        return new RuleEvaluationContext(
            BigDecimal.valueOf(1500),
            "Finance",
            "EXPENSE",
            "USD",
            Map.of(
                "vendor", "Acme Supplies",
                "tags", List.of("urgent", "capex"),
                "code", "EXP-2026",
                "metadata", Map.of("region", "NA")
            )
        );
    }
}
