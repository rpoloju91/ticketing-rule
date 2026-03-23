package com.promotionengine.repository;

import com.promotionengine.entity.RuleHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleHistoryRepository extends JpaRepository<RuleHistory, Long> {

  // All versions of a rule ordered newest first
          List<RuleHistory> findByRuleNameOrderByVersionDesc(
      String ruleName);

  // Specific version for rollback
          Optional<RuleHistory> findByRuleNameAndVersion(
      String ruleName, Integer version);
}