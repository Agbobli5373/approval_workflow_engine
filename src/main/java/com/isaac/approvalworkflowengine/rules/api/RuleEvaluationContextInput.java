package com.isaac.approvalworkflowengine.rules.api;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;

public record RuleEvaluationContextInput(
    BigDecimal amount,
    String department,
    String requestType,
    @Size(min = 3, max = 3) String currency,
    Map<String, Object> payload
) {
}
