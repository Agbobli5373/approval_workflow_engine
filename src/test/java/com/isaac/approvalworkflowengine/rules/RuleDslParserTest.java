package com.isaac.approvalworkflowengine.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isaac.approvalworkflowengine.rules.dsl.RuleDslParser;
import com.isaac.approvalworkflowengine.rules.model.RuleExpression;
import com.isaac.approvalworkflowengine.rules.validation.RuleRegexGuard;
import com.isaac.approvalworkflowengine.shared.error.BadRequestException;
import org.junit.jupiter.api.Test;

class RuleDslParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RuleDslParser parser = new RuleDslParser(new RuleRegexGuard());

    @Test
    void parsesNestedDslAst() throws Exception {
        RuleExpression expression = parser.parse(objectMapper.readTree(
            """
            {
              "all": [
                {"field": "amount", "op": ">=", "value": 1000},
                {
                  "any": [
                    {"field": "department", "op": "==", "value": "Finance"},
                    {"field": "payload.region", "op": "==", "value": "NA"}
                  ]
                }
              ]
            }
            """
        ));

        assertThat(expression).isNotNull();
    }

    @Test
    void rejectsExpressionWithMultipleShapes() throws Exception {
        assertThatThrownBy(() -> parser.parse(objectMapper.readTree(
            """
            {"all": [{"field": "amount", "op": ">", "value": 1}], "field": "department", "op": "==", "value": "Finance"}
            """
        )))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Rule DSL is invalid");
    }

    @Test
    void rejectsUnknownOperator() throws Exception {
        assertThatThrownBy(() -> parser.parse(objectMapper.readTree(
            """
            {"field": "amount", "op": "between", "value": 1000}
            """
        )))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Rule DSL is invalid");
    }

    @Test
    void rejectsUnsafeRegexPattern() throws Exception {
        assertThatThrownBy(() -> parser.parse(objectMapper.readTree(
            """
            {"field": "payload.code", "op": "matches", "value": "(?<=EXP)\\\\d+"}
            """
        )))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Rule DSL is invalid");
    }
}
