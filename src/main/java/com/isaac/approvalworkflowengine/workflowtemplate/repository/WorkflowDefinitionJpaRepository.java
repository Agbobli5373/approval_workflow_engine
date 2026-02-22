package com.isaac.approvalworkflowengine.workflowtemplate.repository;

import com.isaac.approvalworkflowengine.workflowtemplate.repository.entity.WorkflowDefinitionEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowDefinitionJpaRepository extends JpaRepository<WorkflowDefinitionEntity, UUID> {

    Optional<WorkflowDefinitionEntity> findByDefinitionKey(String definitionKey);

    boolean existsByDefinitionKey(String definitionKey);

    boolean existsByRequestType(String requestType);
}
