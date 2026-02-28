package com.isaac.approvalworkflowengine.users.repository;

import com.isaac.approvalworkflowengine.users.repository.entity.DelegationEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DelegationJpaRepository extends JpaRepository<DelegationEntity, UUID>, JpaSpecificationExecutor<DelegationEntity> {

    List<DelegationEntity> findByDelegateUserIdAndActiveTrueAndValidFromLessThanEqualAndValidUntilGreaterThanEqual(
        UUID delegateUserId,
        Instant validFrom,
        Instant validUntil
    );

    List<DelegationEntity> findByDelegatorUserIdAndDelegateUserIdAndActiveTrue(
        UUID delegatorUserId,
        UUID delegateUserId
    );
}
