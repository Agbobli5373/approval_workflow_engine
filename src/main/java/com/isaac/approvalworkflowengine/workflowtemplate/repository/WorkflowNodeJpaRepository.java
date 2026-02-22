package com.isaac.approvalworkflowengine.workflowtemplate.repository;

import com.isaac.approvalworkflowengine.workflowtemplate.repository.entity.WorkflowNodeEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowNodeJpaRepository extends JpaRepository<WorkflowNodeEntity, UUID> {

    List<WorkflowNodeEntity> findByWorkflowVersionIdOrderByNodeKeyAsc(UUID workflowVersionId);

    void deleteByWorkflowVersionId(UUID workflowVersionId);
}
