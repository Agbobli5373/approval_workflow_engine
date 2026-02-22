package com.isaac.approvalworkflowengine.workflowruntime.repository;

import com.isaac.approvalworkflowengine.workflowruntime.repository.entity.RuntimeRequestEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface RuntimeRequestJpaRepository extends JpaRepository<RuntimeRequestEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RuntimeRequestEntity r where r.id = :requestId")
    Optional<RuntimeRequestEntity> findByIdForUpdate(UUID requestId);
}
