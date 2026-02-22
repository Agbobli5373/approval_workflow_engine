package com.isaac.approvalworkflowengine.rules.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RuleSimulationRequest(
    @NotBlank @Size(max = 100) String ruleSetKey,
    @Min(1) int versionNo,
    @NotNull @Valid RuleEvaluationContextInput context
) {
}
