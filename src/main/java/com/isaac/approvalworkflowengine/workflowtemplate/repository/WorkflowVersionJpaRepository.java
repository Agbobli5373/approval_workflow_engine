package com.isaac.approvalworkflowengine.workflowtemplate.repository;

import com.isaac.approvalworkflowengine.workflowtemplate.model.WorkflowVersionStatus;
import com.isaac.approvalworkflowengine.workflowtemplate.repository.entity.WorkflowDefinitionEntity;
import com.isaac.approvalworkflowengine.workflowtemplate.repository.entity.WorkflowVersionEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowVersionJpaRepository extends JpaRepository<WorkflowVersionEntity, UUID> {

    Optional<WorkflowVersionEntity> findTopByWorkflowDefinitionIdOrderByVersionNoDesc(UUID workflowDefinitionId);

    Optional<WorkflowVersionEntity> findByWorkflowDefinitionIdAndStatus(
        UUID workflowDefinitionId,
        WorkflowVersionStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WorkflowVersionEntity w where w.id = :workflowVersionId")
    Optional<WorkflowVersionEntity> findForUpdate(@Param("workflowVersionId") UUID workflowVersionId);

    @Query("""
        select wv.id
        from WorkflowVersionEntity wv, WorkflowDefinitionEntity wd
        where wv.workflowDefinitionId = wd.id
          and upper(wd.requestType) = upper(:requestType)
          and wv.status = :status
        """)
    Optional<UUID> findVersionIdByRequestTypeAndStatus(
        @Param("requestType") String requestType,
        @Param("status") WorkflowVersionStatus status
    );
}
