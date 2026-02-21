package com.isaac.approvalworkflowengine.auth.repository;

import com.isaac.approvalworkflowengine.auth.model.UserAccount;
import com.isaac.approvalworkflowengine.auth.repository.entity.UserEntity;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import com.isaac.approvalworkflowengine.auth.repository.entity.UserRoleEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaUserAccountRepository implements UserAccountRepository {

    private final UserJpaRepository userJpaRepository;

    public JpaUserAccountRepository(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Optional<UserAccount> findByLoginIdentifier(String loginIdentifier) {
        return userJpaRepository.findByExternalSubject(loginIdentifier)
            .or(() -> userJpaRepository.findByEmailIgnoreCase(loginIdentifier))
            .map(this::toUserAccount);
    }

    @Override
    public Optional<UserAccount> findByExternalSubject(String externalSubject) {
        return userJpaRepository.findByExternalSubject(externalSubject)
            .map(this::toUserAccount);
    }

    private UserAccount toUserAccount(UserEntity entity) {
        Set<String> roles = entity.getRoles().stream()
            .map(UserRoleEntity::getRoleCode)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        return new UserAccount(
            entity.getId(),
            entity.getExternalSubject(),
            entity.getEmail(),
            entity.getDisplayName(),
            entity.getDepartment(),
            entity.getEmployeeId(),
            entity.getPasswordHash(),
            entity.isActive(),
            Set.copyOf(roles)
        );
    }
}
