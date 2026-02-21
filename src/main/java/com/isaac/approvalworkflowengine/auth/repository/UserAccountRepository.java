package com.isaac.approvalworkflowengine.auth.repository;

import com.isaac.approvalworkflowengine.auth.model.UserAccount;
import java.util.Optional;

public interface UserAccountRepository {

    Optional<UserAccount> findByLoginIdentifier(String loginIdentifier);

    Optional<UserAccount> findByExternalSubject(String externalSubject);
}
