package com.isaac.approvalworkflowengine.requests.repository;

import com.isaac.approvalworkflowengine.requests.repository.entity.IdempotencyKeyEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyEntity, UUID> {

    Optional<IdempotencyKeyEntity> findByScopeAndKeyValue(String scope, String keyValue);
}
