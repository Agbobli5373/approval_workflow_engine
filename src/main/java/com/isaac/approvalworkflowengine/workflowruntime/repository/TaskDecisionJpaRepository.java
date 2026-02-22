package com.isaac.approvalworkflowengine.workflowruntime.repository;

import com.isaac.approvalworkflowengine.workflowruntime.repository.entity.TaskDecisionEntity;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskDecisionJpaRepository extends JpaRepository<TaskDecisionEntity, UUID> {

    Optional<TaskDecisionEntity> findByTaskIdAndIdempotencyKey(UUID taskId, String idempotencyKey);

    void deleteByTaskIdIn(Collection<UUID> taskIds);
}
