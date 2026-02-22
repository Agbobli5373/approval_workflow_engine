package com.isaac.approvalworkflowengine.rules.model;

import java.math.BigDecimal;
import java.util.Map;

public record RuleEvaluationContext(
    BigDecimal amount,
    String department,
    String requestType,
    String currency,
    Map<String, Object> payload
) {

    public RuleEvaluationContext {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
