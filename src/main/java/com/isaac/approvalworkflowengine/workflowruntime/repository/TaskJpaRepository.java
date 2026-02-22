package com.isaac.approvalworkflowengine.workflowruntime.repository;

import com.isaac.approvalworkflowengine.workflowruntime.model.TaskStatus;
import com.isaac.approvalworkflowengine.workflowruntime.repository.entity.TaskEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface TaskJpaRepository extends JpaRepository<TaskEntity, UUID>, JpaSpecificationExecutor<TaskEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TaskEntity t where t.id = :taskId")
    Optional<TaskEntity> findByIdForUpdate(UUID taskId);

    List<TaskEntity> findByWorkflowInstanceIdAndStatusIn(UUID workflowInstanceId, Collection<TaskStatus> statuses);

    List<TaskEntity> findByWorkflowInstanceIdAndStepKeyInAndStatusIn(
        UUID workflowInstanceId,
        Collection<String> stepKeys,
        Collection<TaskStatus> statuses
    );

    List<TaskEntity> findByWorkflowInstanceId(UUID workflowInstanceId);

    void deleteByWorkflowInstanceId(UUID workflowInstanceId);
}
