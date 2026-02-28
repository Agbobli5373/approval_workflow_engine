package com.isaac.approvalworkflowengine.users.service;

import com.isaac.approvalworkflowengine.shared.api.ApiErrorDetail;
import com.isaac.approvalworkflowengine.shared.error.BadRequestException;
import com.isaac.approvalworkflowengine.users.DelegationPolicyLookup;
import com.isaac.approvalworkflowengine.users.api.DelegationCreateInput;
import com.isaac.approvalworkflowengine.users.api.DelegationResource;
import com.isaac.approvalworkflowengine.users.repository.DelegationJpaRepository;
import com.isaac.approvalworkflowengine.users.repository.entity.DelegationEntity;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DelegationService implements DelegationPolicyLookup {

    private final DelegationJpaRepository delegationJpaRepository;

    public DelegationService(DelegationJpaRepository delegationJpaRepository) {
        this.delegationJpaRepository = delegationJpaRepository;
    }

    @Transactional
    public DelegationResource createDelegation(DelegationCreateInput input, DelegationActor actor) {
        UUID delegatorUserId = input.delegatorUserId() == null ? actor.userId() : input.delegatorUserId();
        UUID delegateUserId = input.delegateUserId();

        if (!actor.workflowAdmin() && !Objects.equals(delegatorUserId, actor.userId())) {
            throw new AccessDeniedException("Only admins can create delegations for other users");
        }

        if (Objects.equals(delegatorUserId, delegateUserId)) {
            throw new BadRequestException(
                "Delegator and delegate cannot be the same",
                List.of(new ApiErrorDetail("delegateUserId", "must be different from delegatorUserId"))
            );
        }

        Instant validFrom = input.validFrom();
        Instant validUntil = input.validUntil();
        if (validUntil.isBefore(validFrom) || validUntil.equals(validFrom)) {
            throw new BadRequestException(
                "Delegation validity window is invalid",
                List.of(new ApiErrorDetail("validUntil", "must be greater than validFrom"))
            );
        }

        boolean allScope = Boolean.TRUE.equals(input.allScope());
        String requestType = normalizeUpper(input.requestType());
        String department = normalizeTrim(input.department());
        String roleCode = normalizeUpper(input.role());

        if (allScope) {
            requestType = null;
            department = null;
            roleCode = null;
        }

        if (!allScope && requestType == null && department == null && roleCode == null) {
            throw new BadRequestException(
                "Delegation scope is required",
                List.of(new ApiErrorDetail("allScope", "set allScope=true or provide requestType/department/role"))
            );
        }

        ensureNoOverlappingConflict(
            delegatorUserId,
            delegateUserId,
            requestType,
            department,
            roleCode,
            allScope,
            validFrom,
            validUntil
        );

        DelegationEntity entity = new DelegationEntity();
        entity.setId(UUID.randomUUID());
        entity.setDelegatorUserId(delegatorUserId);
        entity.setDelegateUserId(delegateUserId);
        entity.setRequestType(requestType);
        entity.setDepartment(department);
        entity.setRoleCode(roleCode);
        entity.setAllScope(allScope);
        entity.setValidFrom(validFrom);
        entity.setValidUntil(validUntil);
        entity.setActive(true);
        entity.setRevokedAt(null);
        entity.setCreatedByUserId(actor.userId());

        return toResource(delegationJpaRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<DelegationResource> listDelegations(Boolean active, Boolean asDelegator, DelegationActor actor) {
        Specification<DelegationEntity> spec = (root, query, builder) -> builder.conjunction();

        if (active == null || active) {
            spec = spec.and((root, query, builder) -> builder.isTrue(root.get("active")));
        } else {
            spec = spec.and((root, query, builder) -> builder.isFalse(root.get("active")));
        }

        if (!actor.workflowAdmin()) {
            if (Boolean.TRUE.equals(asDelegator)) {
                spec = spec.and((root, query, builder) -> builder.equal(root.get("delegatorUserId"), actor.userId()));
            } else if (Boolean.FALSE.equals(asDelegator)) {
                spec = spec.and((root, query, builder) -> builder.equal(root.get("delegateUserId"), actor.userId()));
            } else {
                spec = spec.and((root, query, builder) -> builder.or(
                    builder.equal(root.get("delegatorUserId"), actor.userId()),
                    builder.equal(root.get("delegateUserId"), actor.userId())
                ));
            }
        }

        return delegationJpaRepository.findAll(spec).stream()
            .sorted(java.util.Comparator.comparing(DelegationEntity::getCreatedAt).reversed())
            .map(this::toResource)
            .toList();
    }

    @Transactional
    public DelegationResource revokeDelegation(UUID delegationId, DelegationActor actor) {
        DelegationEntity entity = delegationJpaRepository.findById(delegationId)
            .orElseThrow(() -> new NoSuchElementException("Delegation not found"));

        if (!actor.workflowAdmin() && !Objects.equals(entity.getDelegatorUserId(), actor.userId())) {
            throw new AccessDeniedException("Only admins or the delegator can revoke this delegation");
        }

        if (!entity.isActive()) {
            return toResource(entity);
        }

        entity.setActive(false);
        entity.setRevokedAt(Instant.now());
        return toResource(delegationJpaRepository.save(entity));
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<DelegationAuthorization> resolveDelegation(DelegationTaskContext context) {
        Instant at = context.at() == null ? Instant.now() : context.at();
        List<DelegationEntity> candidates = delegationJpaRepository
            .findByDelegateUserIdAndActiveTrueAndValidFromLessThanEqualAndValidUntilGreaterThanEqual(
                context.delegateUserId(),
                at,
                at
            )
            .stream()
            .filter(entity -> entity.getRevokedAt() == null)
            .filter(entity -> matchesTask(entity, context))
            .toList();

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        if (candidates.size() > 1) {
            throw new IllegalStateException("Multiple active delegations match this task; narrow delegation scope");
        }

        return Optional.of(new DelegationAuthorization(candidates.get(0).getDelegatorUserId()));
    }

    private boolean matchesTask(DelegationEntity entity, DelegationTaskContext context) {
        if (context.assignedUserId() != null && !context.assignedUserId().equals(entity.getDelegatorUserId())) {
            return false;
        }

        if (entity.isAllScope()) {
            return true;
        }

        if (entity.getRequestType() != null && !entity.getRequestType().equalsIgnoreCase(nullToEmpty(context.requestType()))) {
            return false;
        }

        if (entity.getDepartment() != null && !entity.getDepartment().equalsIgnoreCase(nullToEmpty(context.department()))) {
            return false;
        }

        if (entity.getRoleCode() != null && !entity.getRoleCode().equalsIgnoreCase(nullToEmpty(context.assignedRole()))) {
            return false;
        }

        return true;
    }

    private void ensureNoOverlappingConflict(
        UUID delegatorUserId,
        UUID delegateUserId,
        String requestType,
        String department,
        String roleCode,
        boolean allScope,
        Instant validFrom,
        Instant validUntil
    ) {
        List<DelegationEntity> existing = delegationJpaRepository
            .findByDelegatorUserIdAndDelegateUserIdAndActiveTrue(delegatorUserId, delegateUserId);

        for (DelegationEntity entity : existing) {
            if (entity.getRevokedAt() != null) {
                continue;
            }

            boolean sameScope = entity.isAllScope() == allScope
                && Objects.equals(entity.getRequestType(), requestType)
                && Objects.equals(entity.getDepartment(), department)
                && Objects.equals(entity.getRoleCode(), roleCode);

            if (!sameScope) {
                continue;
            }

            boolean overlaps = validFrom.isBefore(entity.getValidUntil()) && validUntil.isAfter(entity.getValidFrom());
            if (overlaps) {
                throw new IllegalStateException("Delegation overlaps with an existing active delegation in the same scope");
            }
        }
    }

    private DelegationResource toResource(DelegationEntity entity) {
        return new DelegationResource(
            entity.getId(),
            entity.getDelegatorUserId(),
            entity.getDelegateUserId(),
            entity.getRequestType(),
            entity.getDepartment(),
            entity.getRoleCode(),
            entity.isAllScope(),
            entity.getValidFrom(),
            entity.getValidUntil(),
            entity.isActive(),
            entity.getRevokedAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private String normalizeUpper(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeTrim(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
