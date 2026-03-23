package com.promotionengine.controller;

import com.promotionengine.dto.Cart;
import com.promotionengine.dto.DiscountResult;
import com.promotionengine.entity.DiscountAuditLog;
import com.promotionengine.entity.RuleDefinition;
import com.promotionengine.entity.RuleHistory;
import com.promotionengine.repository.DiscountAuditLogRepository;
import com.promotionengine.repository.RuleDefinitionRepository;
import com.promotionengine.repository.RuleHistoryRepository;
import com.promotionengine.service.DroolsEngineService;
import com.promotionengine.service.DroolsRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/engine")
@RequiredArgsConstructor
public class RuleEngineController {

    private final DroolsEngineService droolsEngineService;
    private final DroolsRuleService droolsRuleService;
    private final RuleHistoryRepository ruleHistoryRepository;
    private final RuleDefinitionRepository ruleDefinitionRepository;
    private final DiscountAuditLogRepository auditLogRepository;

    // Apply discounts to cart
    @PostMapping("/apply")
    public ResponseEntity<DiscountResult> apply(@RequestBody Cart cart) {
        return ResponseEntity.ok(droolsEngineService.applyRules(cart));
    }

    // Force reload all rules
    @PostMapping("/reload")
    public ResponseEntity<String> reload() {
        droolsEngineService.reloadRules();
        return ResponseEntity.ok("Rules reloaded successfully.");
    }

    // Get all active rules
    @GetMapping("/rules")
    public ResponseEntity<List<RuleDefinition>> getAllRules() {
        return ResponseEntity.ok(ruleDefinitionRepository.findByActiveTrue());
    }

    // Get rule history by rule name
    @GetMapping("/rules/{ruleName}/history")
    public ResponseEntity<List<RuleHistory>> getRuleHistory(@PathVariable String ruleName) {
        return ResponseEntity.ok(ruleHistoryRepository.findByRuleNameOrderByVersionDesc(ruleName));
    }

    // Rollback rule to specific version
    @PostMapping("/rules/{ruleName}/rollback/{version}")
    public ResponseEntity<RuleDefinition> rollback(@PathVariable String ruleName, @PathVariable Integer version, @RequestParam(defaultValue = "ADMIN") String changedBy) {
        return ResponseEntity.ok(droolsRuleService.rollbackRule(ruleName, version, changedBy));
    }

    // Get audit logs for a customer
    @GetMapping("/audit/{customerId}")
    public ResponseEntity<List<DiscountAuditLog>> getAuditLogs(@PathVariable String customerId) {
        return ResponseEntity.ok(auditLogRepository.findByCustomerIdOrderByAppliedAtDesc(customerId));
    }
}