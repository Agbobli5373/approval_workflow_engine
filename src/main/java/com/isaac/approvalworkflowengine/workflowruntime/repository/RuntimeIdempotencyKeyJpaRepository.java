package com.isaac.approvalworkflowengine.workflowruntime.repository;

import com.isaac.approvalworkflowengine.workflowruntime.repository.entity.RuntimeIdempotencyKeyEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuntimeIdempotencyKeyJpaRepository extends JpaRepository<RuntimeIdempotencyKeyEntity, UUID> {

    Optional<RuntimeIdempotencyKeyEntity> findByScopeAndKeyValue(String scope, String keyValue);
}
