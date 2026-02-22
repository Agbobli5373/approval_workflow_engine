package com.isaac.approvalworkflowengine.rules.api;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record RuleSetVersionInput(
    @NotNull Map<String, Object> dsl
) {
}
