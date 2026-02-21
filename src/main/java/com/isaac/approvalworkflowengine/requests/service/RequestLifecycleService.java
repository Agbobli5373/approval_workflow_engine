package com.isaac.approvalworkflowengine.requests.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isaac.approvalworkflowengine.requests.api.AttachmentMetadata;
import com.isaac.approvalworkflowengine.requests.api.PageMetadata;
import com.isaac.approvalworkflowengine.requests.api.PagedRequestResource;
import com.isaac.approvalworkflowengine.requests.api.RequestCreateInput;
import com.isaac.approvalworkflowengine.requests.api.RequestResource;
import com.isaac.approvalworkflowengine.requests.api.RequestUpdateInput;
import com.isaac.approvalworkflowengine.requests.model.RequestStatus;
import com.isaac.approvalworkflowengine.requests.repository.IdempotencyKeyJpaRepository;
import com.isaac.approvalworkflowengine.requests.repository.RequestJpaRepository;
import com.isaac.approvalworkflowengine.requests.repository.RequestStatusTransitionJpaRepository;
import com.isaac.approvalworkflowengine.requests.repository.entity.IdempotencyKeyEntity;
import com.isaac.approvalworkflowengine.requests.repository.entity.RequestEntity;
import com.isaac.approvalworkflowengine.requests.repository.entity.RequestStatusTransitionEntity;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RequestLifecycleService {

    private static final String SUBMIT_SCOPE = "REQUEST_SUBMIT";
    private static final String CANCEL_SCOPE = "REQUEST_CANCEL";

    private static final Map<String, String> SORT_FIELD_MAPPING = Map.of(
        "createdAt", "createdAt",
        "updatedAt", "updatedAt",
        "status", "status",
        "requestType", "requestType",
        "title", "title"
    );

    private final RequestJpaRepository requestJpaRepository;
    private final RequestStatusTransitionJpaRepository requestStatusTransitionJpaRepository;
    private final IdempotencyKeyJpaRepository idempotencyKeyJpaRepository;
    private final RequestWorkflowVersionResolver requestWorkflowVersionResolver;
    private final ObjectMapper objectMapper;

    public RequestLifecycleService(
        RequestJpaRepository requestJpaRepository,
        RequestStatusTransitionJpaRepository requestStatusTransitionJpaRepository,
        IdempotencyKeyJpaRepository idempotencyKeyJpaRepository,
        RequestWorkflowVersionResolver requestWorkflowVersionResolver,
        ObjectMapper objectMapper
    ) {
        this.requestJpaRepository = requestJpaRepository;
        this.requestStatusTransitionJpaRepository = requestStatusTransitionJpaRepository;
        this.idempotencyKeyJpaRepository = idempotencyKeyJpaRepository;
        this.requestWorkflowVersionResolver = requestWorkflowVersionResolver;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RequestResource create(RequestCreateInput input, RequestActor actor) {
        RequestEntity request = new RequestEntity();
        request.setId(UUID.randomUUID());
        request.setRequestType(input.requestType().trim());
        request.setTitle(input.title().trim());
        request.setDescription(trimToNull(input.description()));
        request.setPayloadJson(writeJson(input.payload()));
        request.setAmount(input.amount());
        request.setCurrency(normalizeCurrency(input.currency()));
        request.setDepartment(trimToNull(input.department()));
        request.setCostCenter(trimToNull(input.costCenter()));
        request.setAttachmentsJson(writeJson(input.attachments() == null ? List.of() : input.attachments()));
        request.setStatus(RequestStatus.DRAFT);
        request.setRequestorUserId(actor.userId());

        return toResource(requestJpaRepository.save(request));
    }

    @Transactional
    public RequestResource update(UUID requestId, RequestUpdateInput input, RequestActor actor) {
        RequestEntity request = loadAuthorizedRequest(requestId, actor);

        if (!request.getStatus().isEditable()) {
            throw new IllegalStateException("Request cannot be edited in status " + request.getStatus());
        }

        if (input.title() != null) {
            request.setTitle(input.title().trim());
        }
        if (input.description() != null) {
            request.setDescription(trimToNull(input.description()));
        }
        if (input.payload() != null) {
            request.setPayloadJson(writeJson(input.payload()));
        }
        if (input.amount() != null) {
            request.setAmount(input.amount());
        }
        if (input.currency() != null) {
            request.setCurrency(normalizeCurrency(input.currency()));
        }
        if (input.department() != null) {
            request.setDepartment(trimToNull(input.department()));
        }
        if (input.costCenter() != null) {
            request.setCostCenter(trimToNull(input.costCenter()));
        }
        if (input.attachments() != null) {
            request.setAttachmentsJson(writeJson(input.attachments()));
        }

        return toResource(requestJpaRepository.save(request));
    }

    @Transactional(readOnly = true)
    public RequestResource get(UUID requestId, RequestActor actor) {
        return toResource(loadAuthorizedRequest(requestId, actor));
    }

    @Transactional(readOnly = true)
    public PagedRequestResource list(
        RequestStatus status,
        String requestType,
        Instant createdFrom,
        Instant createdTo,
        int page,
        int size,
        String sort,
        RequestActor actor
    ) {
        Pageable pageable = PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), 200),
            parseSort(sort)
        );

        Specification<RequestEntity> specification = (root, query, builder) -> builder.conjunction();

        if (status != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("status"), status));
        }
        if (StringUtils.hasText(requestType)) {
            String normalizedRequestType = requestType.trim().toLowerCase(Locale.ROOT);
            specification = specification.and((root, query, builder) ->
                builder.equal(builder.lower(root.get("requestType")), normalizedRequestType)
            );
        }
        if (createdFrom != null) {
            specification = specification.and((root, query, builder) ->
                builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom)
            );
        }
        if (createdTo != null) {
            specification = specification.and((root, query, builder) ->
                builder.lessThanOrEqualTo(root.get("createdAt"), createdTo)
            );
        }
        if (!actor.workflowAdmin()) {
            specification = specification.and((root, query, builder) ->
                builder.equal(root.get("requestorUserId"), actor.userId())
            );
        }

        Page<RequestEntity> requestPage = requestJpaRepository.findAll(specification, pageable);
        List<RequestResource> resources = requestPage.getContent().stream()
            .map(this::toResource)
            .toList();

        return new PagedRequestResource(
            resources,
            new PageMetadata(
                requestPage.getNumber(),
                requestPage.getSize(),
                requestPage.getTotalElements(),
                requestPage.getTotalPages()
            )
        );
    }

    @Transactional
    public RequestResource submit(UUID requestId, String idempotencyKey, RequestActor actor) {
        return performIdempotentTransition(SUBMIT_SCOPE, requestId, idempotencyKey, actor, () -> {
            RequestEntity request = loadAuthorizedRequest(requestId, actor);
            RequestStatus fromStatus = request.getStatus();

            if (!fromStatus.isSubmittable()) {
                throw new IllegalStateException("Request cannot be submitted in status " + fromStatus);
            }

            UUID workflowVersionId = request.getWorkflowVersionId();
            if (workflowVersionId == null) {
                workflowVersionId = requestWorkflowVersionResolver
                    .resolveActiveWorkflowVersionId(request.getRequestType())
                    .orElseThrow(() -> new IllegalStateException(
                        "No active workflow version configured for request type " + request.getRequestType()
                    ));
                request.setWorkflowVersionId(workflowVersionId);
            }

            request.setStatus(RequestStatus.SUBMITTED);
            RequestEntity saved = requestJpaRepository.save(request);

            requestStatusTransitionJpaRepository.save(new RequestStatusTransitionEntity(
                UUID.randomUUID(),
                saved.getId(),
                fromStatus,
                RequestStatus.SUBMITTED,
                actor.subject(),
                "SUBMIT",
                Instant.now()
            ));

            return saved;
        });
    }

    @Transactional
    public RequestResource cancel(UUID requestId, String idempotencyKey, RequestActor actor) {
        return performIdempotentTransition(CANCEL_SCOPE, requestId, idempotencyKey, actor, () -> {
            RequestEntity request = loadAuthorizedRequest(requestId, actor);
            RequestStatus fromStatus = request.getStatus();

            if (!fromStatus.isCancellable()) {
                throw new IllegalStateException("Request cannot be cancelled in status " + fromStatus);
            }

            request.setStatus(RequestStatus.CANCELLED);
            RequestEntity saved = requestJpaRepository.save(request);

            requestStatusTransitionJpaRepository.save(new RequestStatusTransitionEntity(
                UUID.randomUUID(),
                saved.getId(),
                fromStatus,
                RequestStatus.CANCELLED,
                actor.subject(),
                "CANCEL",
                Instant.now()
            ));

            return saved;
        });
    }

    private RequestEntity loadAuthorizedRequest(UUID requestId, RequestActor actor) {
        RequestEntity request = requestJpaRepository.findById(requestId)
            .orElseThrow(() -> new NoSuchElementException("Request not found"));

        if (!actor.workflowAdmin() && !Objects.equals(request.getRequestorUserId(), actor.userId())) {
            throw new AccessDeniedException("Access denied");
        }

        return request;
    }

    private RequestResource performIdempotentTransition(
        String scope,
        UUID requestId,
        String idempotencyKey,
        RequestActor actor,
        Supplier<RequestEntity> transitionSupplier
    ) {
        String requestHash = computeRequestHash(scope, requestId);
        IdempotencyKeyEntity existing = idempotencyKeyJpaRepository.findByScopeAndKeyValue(scope, idempotencyKey)
            .orElse(null);

        if (existing != null) {
            ensureMatchingRequestHash(existing, requestHash);
            return toResource(loadAuthorizedRequest(requestId, actor));
        }

        RequestEntity transitioned = transitionSupplier.get();
        String responseJson = writeJson(Map.of(
            "requestId", transitioned.getId().toString(),
            "status", transitioned.getStatus().name()
        ));

        IdempotencyKeyEntity record = new IdempotencyKeyEntity(
            UUID.randomUUID(),
            scope,
            idempotencyKey,
            requestHash,
            responseJson,
            Instant.now()
        );

        try {
            idempotencyKeyJpaRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException conflict) {
            IdempotencyKeyEntity persisted = idempotencyKeyJpaRepository.findByScopeAndKeyValue(scope, idempotencyKey)
                .orElseThrow(() -> conflict);
            ensureMatchingRequestHash(persisted, requestHash);
        }

        return toResource(loadAuthorizedRequest(requestId, actor));
    }

    private void ensureMatchingRequestHash(IdempotencyKeyEntity existing, String requestHash) {
        if (!Objects.equals(existing.getRequestHash(), requestHash)) {
            throw new IllegalStateException("Idempotency key has already been used for another request");
        }
    }

    private String computeRequestHash(String scope, UUID requestId) {
        byte[] bytes = (scope + ":" + requestId).getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }

    private Sort parseSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] tokens = sort.split(",", 2);
        String requestedField = tokens[0].trim();
        String mappedField = SORT_FIELD_MAPPING.getOrDefault(requestedField, "createdAt");
        Sort.Direction direction = Sort.Direction.DESC;

        if (tokens.length > 1 && tokens[1].equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        }

        return Sort.by(direction, mappedField);
    }

    private String normalizeCurrency(String currency) {
        if (!StringUtils.hasText(currency)) {
            return null;
        }
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize request JSON payload", exception);
        }
    }

    private Map<String, Object> readPayload(String payloadJson) {
        try {
            if (!StringUtils.hasText(payloadJson)) {
                return Map.of();
            }
            return objectMapper.readValue(payloadJson, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize request payload JSON", exception);
        }
    }

    private List<AttachmentMetadata> readAttachments(String attachmentsJson) {
        try {
            if (!StringUtils.hasText(attachmentsJson)) {
                return List.of();
            }
            return objectMapper.readValue(attachmentsJson, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize request attachments JSON", exception);
        }
    }

    private RequestResource toResource(RequestEntity request) {
        return new RequestResource(
            request.getId(),
            request.getRequestType(),
            request.getTitle(),
            request.getDescription(),
            readPayload(request.getPayloadJson()),
            request.getAmount(),
            request.getCurrency(),
            request.getDepartment(),
            request.getCostCenter(),
            readAttachments(request.getAttachmentsJson()),
            request.getStatus(),
            request.getWorkflowVersionId(),
            request.getCreatedAt(),
            request.getUpdatedAt()
        );
    }
}
