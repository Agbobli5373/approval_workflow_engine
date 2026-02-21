package com.isaac.approvalworkflowengine.auth.repository;

import com.isaac.approvalworkflowengine.auth.repository.entity.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    @EntityGraph(attributePaths = "roles")
    Optional<UserEntity> findByExternalSubject(String externalSubject);

    @EntityGraph(attributePaths = "roles")
    Optional<UserEntity> findByEmailIgnoreCase(String email);
}
