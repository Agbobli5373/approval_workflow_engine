package com.isaac.approvalworkflowengine.workflowruntime.repository;

import com.isaac.approvalworkflowengine.workflowruntime.repository.entity.WorkflowInstanceEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface WorkflowInstanceJpaRepository extends JpaRepository<WorkflowInstanceEntity, UUID> {

    Optional<WorkflowInstanceEntity> findByRequestId(UUID requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select wi from WorkflowInstanceEntity wi where wi.requestId = :requestId")
    Optional<WorkflowInstanceEntity> findByRequestIdForUpdate(UUID requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select wi from WorkflowInstanceEntity wi where wi.id = :workflowInstanceId")
    Optional<WorkflowInstanceEntity> findByIdForUpdate(UUID workflowInstanceId);
}
