package com.isaac.approvalworkflowengine.rules.api;

import com.isaac.approvalworkflowengine.rules.service.RuleSetActor;
import com.isaac.approvalworkflowengine.rules.service.RuleSetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(path = "/api", version = "1.0")
@PreAuthorize("hasRole('WORKFLOW_ADMIN')")
public class RuleSetController {

    private final RuleSetService ruleSetService;

    public RuleSetController(RuleSetService ruleSetService) {
        this.ruleSetService = ruleSetService;
    }

    @PostMapping("/rule-sets/{ruleSetKey}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public RuleSetVersionResource createVersion(
        @PathVariable String ruleSetKey,
        @Valid @RequestBody RuleSetVersionInput input,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ruleSetService.createVersion(ruleSetKey, input, RuleSetActor.fromJwt(jwt));
    }

    @GetMapping("/rule-sets/{ruleSetKey}/versions/{versionNo}")
    public RuleSetVersionResource getVersion(
        @PathVariable String ruleSetKey,
        @PathVariable @Min(1) int versionNo
    ) {
        return ruleSetService.getVersion(ruleSetKey, versionNo);
    }

    @GetMapping("/rule-sets/{ruleSetKey}/versions")
    public PagedRuleSetVersionResource listVersions(
        @PathVariable String ruleSetKey,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size
    ) {
        return ruleSetService.listVersions(ruleSetKey, page, size);
    }

    @PostMapping("/rule-sets/simulations")
    public RuleSimulationResponse simulate(@Valid @RequestBody RuleSimulationRequest input) {
        return ruleSetService.simulate(input);
    }
}
