package com.promotionengine.repository;

import com.promotionengine.entity.RuleDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleDefinitionRepository extends JpaRepository<RuleDefinition, Long> {

    // All active rules — loaded into KieContainer
    List<RuleDefinition> findByActiveTrue();

    // Find specific active rule by type + referenceId
    Optional<RuleDefinition> findByRuleTypeAndReferenceIdAndActiveTrue(String ruleType, Long referenceId);

    // Find by rule name
    Optional<RuleDefinition> findByRuleNameAndActiveTrue(String ruleName);
}