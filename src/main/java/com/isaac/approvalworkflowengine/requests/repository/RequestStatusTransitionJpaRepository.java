package com.isaac.approvalworkflowengine.requests.repository;

import com.isaac.approvalworkflowengine.requests.repository.entity.RequestStatusTransitionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestStatusTransitionJpaRepository extends JpaRepository<RequestStatusTransitionEntity, UUID> {
}
