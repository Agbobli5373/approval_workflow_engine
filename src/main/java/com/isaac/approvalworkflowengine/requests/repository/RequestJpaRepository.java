package com.isaac.approvalworkflowengine.requests.repository;

import com.isaac.approvalworkflowengine.requests.repository.entity.RequestEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RequestJpaRepository extends JpaRepository<RequestEntity, UUID>, JpaSpecificationExecutor<RequestEntity> {
}
