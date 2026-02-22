package com.isaac.approvalworkflowengine.requests.api;

import com.isaac.approvalworkflowengine.requests.model.RequestStatus;
import com.isaac.approvalworkflowengine.requests.service.RequestActor;
import com.isaac.approvalworkflowengine.requests.service.RequestLifecycleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(path = "/api/requests", version = "1.0")
public class RequestController {

    private final RequestLifecycleService requestLifecycleService;

    public RequestController(RequestLifecycleService requestLifecycleService) {
        this.requestLifecycleService = requestLifecycleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequestResource create(
        @Valid @RequestBody RequestCreateInput input,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return requestLifecycleService.create(input, RequestActor.fromJwt(jwt));
    }

    @PatchMapping("/{requestId}")
    public RequestResource update(
        @PathVariable UUID requestId,
        @Valid @RequestBody RequestUpdateInput input,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return requestLifecycleService.update(requestId, input, RequestActor.fromJwt(jwt));
    }

    @PostMapping("/{requestId}/submit")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RequestResource submit(
        @PathVariable UUID requestId,
        @RequestHeader("Idempotency-Key") @Size(min = 8, max = 120) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return requestLifecycleService.submit(requestId, idempotencyKey, RequestActor.fromJwt(jwt));
    }

    @PostMapping("/{requestId}/cancel")
    public RequestResource cancel(
        @PathVariable UUID requestId,
        @RequestHeader("Idempotency-Key") @Size(min = 8, max = 120) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return requestLifecycleService.cancel(requestId, idempotencyKey, RequestActor.fromJwt(jwt));
    }

    @GetMapping("/{requestId}")
    public RequestResource get(
        @PathVariable UUID requestId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return requestLifecycleService.get(requestId, RequestActor.fromJwt(jwt));
    }

    @GetMapping
    public PagedRequestResource list(
        @RequestParam(required = false) RequestStatus status,
        @RequestParam(required = false) String requestType,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return requestLifecycleService.list(
            status,
            requestType,
            createdFrom,
            createdTo,
            page,
            size,
            sort,
            RequestActor.fromJwt(jwt)
        );
    }
}
