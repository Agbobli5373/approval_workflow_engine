package com.isaac.approvalworkflowengine.workflowruntime.repository;

import com.isaac.approvalworkflowengine.workflowruntime.repository.entity.RuntimeRequestStatusTransitionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuntimeRequestStatusTransitionJpaRepository
    extends JpaRepository<RuntimeRequestStatusTransitionEntity, UUID> {
}
