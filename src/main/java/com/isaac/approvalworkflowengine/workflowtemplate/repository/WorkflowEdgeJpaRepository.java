package com.isaac.approvalworkflowengine.workflowtemplate.repository;

import com.isaac.approvalworkflowengine.workflowtemplate.repository.entity.WorkflowEdgeEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowEdgeJpaRepository extends JpaRepository<WorkflowEdgeEntity, UUID> {

    List<WorkflowEdgeEntity> findByWorkflowVersionIdOrderByFromNodeKeyAscToNodeKeyAsc(UUID workflowVersionId);

    void deleteByWorkflowVersionId(UUID workflowVersionId);
}
