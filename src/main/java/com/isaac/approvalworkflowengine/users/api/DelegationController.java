package com.isaac.approvalworkflowengine.users.api;

import com.isaac.approvalworkflowengine.users.service.DelegationActor;
import com.isaac.approvalworkflowengine.users.service.DelegationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
@RequestMapping(path = "/api/delegations", version = "1.0")
public class DelegationController {

    private final DelegationService delegationService;

    public DelegationController(DelegationService delegationService) {
        this.delegationService = delegationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DelegationResource create(
        @Valid @RequestBody DelegationCreateInput input,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return delegationService.createDelegation(input, DelegationActor.fromJwt(jwt));
    }

    @GetMapping
    public List<DelegationResource> list(
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) Boolean asDelegator,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return delegationService.listDelegations(active, asDelegator, DelegationActor.fromJwt(jwt));
    }

    @PostMapping("/{delegationId}/revoke")
    public DelegationResource revoke(
        @PathVariable UUID delegationId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return delegationService.revokeDelegation(delegationId, DelegationActor.fromJwt(jwt));
    }
}
