package com.isaac.approvalworkflowengine.rules.repository;

import com.isaac.approvalworkflowengine.rules.repository.entity.RuleSetEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleSetJpaRepository extends JpaRepository<RuleSetEntity, UUID> {

    Optional<RuleSetEntity> findTopByRuleSetKeyOrderByVersionNoDesc(String ruleSetKey);

    Optional<RuleSetEntity> findByRuleSetKeyAndVersionNo(String ruleSetKey, int versionNo);

    Page<RuleSetEntity> findByRuleSetKeyOrderByVersionNoDesc(String ruleSetKey, Pageable pageable);

    boolean existsByRuleSetKeyAndVersionNo(String ruleSetKey, int versionNo);
}
