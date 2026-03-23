package com.promotionengine.service;

import com.promotionengine.entity.*;
import com.promotionengine.enums.DiscountType;
import com.promotionengine.enums.UserType;
import com.promotionengine.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DroolsRuleService {

    private final RuleDefinitionRepository ruleDefinitionRepository;
    private final RuleHistoryRepository ruleHistoryRepository;

    // ✅ Use ApplicationContext to avoid circular dependency
    // DroolsEngineService → DroolsRuleService (for reload)
    // DroolsRuleService   → DroolsEngineService (for reload)
    // Break cycle by lazy lookup via ApplicationContext
    private final ApplicationContext applicationContext;

    // Lazy getter — avoids circular dependency at startup
    private DroolsEngineService getEngineService() {
        return applicationContext.getBean(DroolsEngineService.class);
    }

    // ════════════════════════════════════════
    //  GENERATE PROMOTION RULE
    // ════════════════════════════════════════
    @Transactional
    public RuleDefinition generatePromotionRule(Promotion p, String changedBy) {

        String drl = buildPromotionDRL(p);
        RuleDefinition rule = findOrNew("PROMOTION", p.getId());
        boolean isNew = rule.getId() == null;
        int nextVersion = isNew ? 1 : rule.getVersion() + 1;

        if (!isNew) saveHistory(rule, "UPDATED", changedBy);

        String ruleName = p.getRedemptionMethod() == Promotion.RedemptionMethod.AUTOMATIC ? "PROMO_AUTO_" + p.getId() : "PROMO_" + p.getPromoCode();

        rule.setRuleName(ruleName);
        rule.setRuleType("PROMOTION");
        rule.setReferenceId(p.getId());
        rule.setDrlContent(drl);
        rule.setVersion(nextVersion);
        rule.setActive(true);
        rule.setCreatedBy(changedBy);

        RuleDefinition saved = ruleDefinitionRepository.save(rule);
        if (isNew) saveHistory(saved, "CREATED", changedBy);

        // ✅ Lazy call — no circular dependency
        getEngineService().reloadRules();
        log.info("Promotion rule generated: {}", ruleName);
        return saved;
    }

    // ════════════════════════════════════════
    //  GENERATE SYSTEM DISCOUNT RULE
    // ════════════════════════════════════════
    @Transactional
    public RuleDefinition generateSystemDiscountRule(SystemDiscount sd, String changedBy) {

        String drl = buildSystemDiscountDRL(sd);
        RuleDefinition rule = findOrNew("SYSTEM_DISCOUNT", sd.getId());
        boolean isNew = rule.getId() == null;
        int nextVersion = isNew ? 1 : rule.getVersion() + 1;

        if (!isNew) saveHistory(rule, "UPDATED", changedBy);

        String ruleName = "SYSDISCOUNT_" + sd.getTitle();
        rule.setRuleName(ruleName);
        rule.setRuleType("SYSTEM_DISCOUNT");
        rule.setReferenceId(sd.getId());
        rule.setDrlContent(drl);
        rule.setVersion(nextVersion);
        rule.setActive(true);
        rule.setCreatedBy(changedBy);

        RuleDefinition saved = ruleDefinitionRepository.save(rule);
        if (isNew) saveHistory(saved, "CREATED", changedBy);

        // ✅ Lazy call
        getEngineService().reloadRules();
        log.info("SystemDiscount rule generated: {}", ruleName);
        return saved;
    }

    // ════════════════════════════════════════
    //  DEACTIVATE RULE
    // ════════════════════════════════════════
    @Transactional
    public void deactivateRule(String ruleType, Long referenceId, String changedBy) {
        ruleDefinitionRepository.findByRuleTypeAndReferenceIdAndActiveTrue(ruleType, referenceId).ifPresent(rule -> {
            saveHistory(rule, "DEACTIVATED", changedBy);
            rule.setActive(false);
            ruleDefinitionRepository.save(rule);
            // ✅ Lazy call
            getEngineService().reloadRules();
            log.info("Rule deactivated: {}", rule.getRuleName());
        });
    }

    // ════════════════════════════════════════
    //  ROLLBACK RULE
    // ════════════════════════════════════════
    @Transactional
    public RuleDefinition rollbackRule(String ruleName, Integer toVersion, String changedBy) {

        RuleHistory history = ruleHistoryRepository.findByRuleNameAndVersion(ruleName, toVersion).orElseThrow(() -> new RuntimeException("Version " + toVersion + " not found for: " + ruleName));

        RuleDefinition current = ruleDefinitionRepository.findByRuleNameAndActiveTrue(ruleName).orElseThrow(() -> new RuntimeException("Active rule not found: " + ruleName));

        saveHistory(current, "ROLLED_BACK", changedBy);

        int nextVersion = current.getVersion() + 1;
        current.setDrlContent(history.getDrlContent());
        current.setVersion(nextVersion);
        current.setCreatedBy(changedBy);

        RuleDefinition saved = ruleDefinitionRepository.save(current);

        RuleHistory rollback = new RuleHistory();
        rollback.setRuleName(ruleName);
        rollback.setRuleType(current.getRuleType());
        rollback.setReferenceId(current.getReferenceId());
        rollback.setDrlContent(history.getDrlContent());
        rollback.setVersion(nextVersion);
        rollback.setAction("ROLLBACK_TO_V" + toVersion);
        rollback.setChangedBy(changedBy);
        ruleHistoryRepository.save(rollback);

        // ✅ Lazy call
        getEngineService().reloadRules();
        log.info("Rule {} rolled back to v{} now v{}", ruleName, toVersion, nextVersion);
        return saved;
    }

    // ════════════════════════════════════════
    //  BUILD PROMOTION DRL
    // ════════════════════════════════════════
    private String buildPromotionDRL(Promotion p) {
        StringBuilder drl = new StringBuilder();

        String ruleName = p.getRedemptionMethod() == Promotion.RedemptionMethod.AUTOMATIC ? "PROMO_AUTO_" + p.getId() : "PROMO_" + p.getPromoCode();

        drl.append("package com.promotionengine.rules;\n\n");
        drl.append("import com.promotionengine.dto.Cart;\n");
        drl.append("import com.promotionengine.dto.CartItem;\n");
        drl.append("import com.promotionengine.dto.DiscountResult;\n\n");

        drl.append("rule \"").append(ruleName).append("\"\n");
        drl.append("    salience ").append(convertPriority(p.getPriority())).append("\n");
        drl.append("    no-loop true\n");   // ← keep this
        drl.append("    when\n");

        drl.append("        $result : DiscountResult(\n");
        drl.append("            promoStackLocked == false,\n");
        drl.append("            isRuleAlreadyFired(\"").append(ruleName).append("\") == false\n");       // ← CRITICAL
        drl.append("        )\n");

        // Cart conditions
        drl.append("        $cart : Cart(\n");

        if (p.getRedemptionMethod() == Promotion.RedemptionMethod.PROMO_CODE) {
            drl.append("            hasPromoCode(\"").append(p.getPromoCode()).append("\") == true");
        } else {
            drl.append("            cartTotal > 0");
        }

        // Date conditions
        if (p.getStartDate() != null) {
            drl.append(",\n").append("            currentDateTime != null,\n").append("            currentDateTime >= ").append("java.time.LocalDateTime.of(").append(p.getStartDate().getYear()).append(", ").append(p.getStartDate().getMonthValue()).append(", ").append(p.getStartDate().getDayOfMonth()).append(", ").append(p.getStartDate().getHour()).append(", ").append(p.getStartDate().getMinute()).append(", ").append(p.getStartDate().getSecond()).append(")");
        }
        if (p.getEndDate() != null) {
            drl.append(",\n").append("            currentDateTime != null,\n").append("            currentDateTime <= ").append("java.time.LocalDateTime.of(").append(p.getEndDate().getYear()).append(", ").append(p.getEndDate().getMonthValue()).append(", ").append(p.getEndDate().getDayOfMonth()).append(", ").append(p.getEndDate().getHour()).append(", ").append(p.getEndDate().getMinute()).append(", ").append(p.getEndDate().getSecond()).append(")");
        }

        // User type
        if (p.getUserType() != null && p.getUserType() != Promotion.UserType.ALL_USERS) {
            String ut = p.getUserType() == Promotion.UserType.MEMBERS ? "MEMBER" : "NON_MEMBER";
            drl.append(",\n").append("            userType != null,\n").append("            userType == \"").append(ut).append("\"");
        }

        // Channel
        if (Boolean.TRUE.equals(p.getChannelWeb()) && !Boolean.TRUE.equals(p.getChannelPos())) {
            drl.append(",\n").append("            channel == \"WEB\"");
        } else if (Boolean.TRUE.equals(p.getChannelPos()) && !Boolean.TRUE.equals(p.getChannelWeb())) {
            drl.append(",\n").append("            channel == \"POS\"");
        }

        drl.append("\n        )\n");
        drl.append("    then\n");

        // Discount calculation
        if (Boolean.TRUE.equals(p.getApplyFullCart())) {
            appendFullCartDiscount(drl, p, ruleName);
        } else if (p.getTicketTitles() != null && !p.getTicketTitles().isEmpty()) {
            appendTicketLevelDiscount(drl, p, ruleName);
        } else if (p.getCategory() != null) {
            appendCategoryDiscount(drl, p, ruleName);
        } else {
            appendFullCartDiscount(drl, p, ruleName);
        }

        // Non-stackable sets lock
        if (!Boolean.TRUE.equals(p.getStackable())) {
            drl.append("        $result.setPromoStackLocked(true);\n");
        }

        drl.append("        update($result);\n");
        drl.append("end\n");

        return drl.toString();
    }

    private String buildSystemDiscountDRL(SystemDiscount sd) {
        StringBuilder drl = new StringBuilder();

        String ruleName = "SYSDISCOUNT_" + sd.getTitle();

        drl.append("package com.promotionengine.rules;\n\n");
        drl.append("import com.promotionengine.dto.Cart;\n");
        drl.append("import com.promotionengine.dto.DiscountResult;\n\n");

        drl.append("rule \"").append(ruleName).append("\"\n");
        drl.append("    salience ").append(sd.getPriority() != null ? sd.getPriority() : 50).append("\n");
        drl.append("    no-loop true\n");
        drl.append("    when\n");

        drl.append("        $result : DiscountResult(\n");
        drl.append("            nonStackableLocked == false,\n");
        drl.append("            isRuleAlreadyFired(\"").append(ruleName).append("\") == false");         // ← CRITICAL

        if (sd.getStackGroup() != null && !sd.getStackGroup().isBlank()) {
            drl.append(",\n").append("            isStackGroupAlreadyFired(\"").append(sd.getStackGroup()).append("\") == false");
        }
        drl.append("\n        )\n");

        drl.append("        $cart : Cart(\n");
        drl.append("            cartTotal > 0");

        if (sd.getStartDate() != null) {
            drl.append(",\n").append("            currentDateTime != null,\n").append("            currentDateTime >= ").append("java.time.LocalDateTime.of(").append(sd.getStartDate().getYear()).append(", ").append(sd.getStartDate().getMonthValue()).append(", ").append(sd.getStartDate().getDayOfMonth()).append(", ").append(sd.getStartDate().getHour()).append(", ").append(sd.getStartDate().getMinute()).append(", ").append(sd.getStartDate().getSecond()).append(")");
        }
        if (sd.getEndDate() != null) {
            drl.append(",\n").append("            currentDateTime != null,\n").append("            currentDateTime <= ").append("java.time.LocalDateTime.of(").append(sd.getEndDate().getYear()).append(", ").append(sd.getEndDate().getMonthValue()).append(", ").append(sd.getEndDate().getDayOfMonth()).append(", ").append(sd.getEndDate().getHour()).append(", ").append(sd.getEndDate().getMinute()).append(", ").append(sd.getEndDate().getSecond()).append(")");
        }

        if (sd.getUserType() != null && sd.getUserType() != UserType.ALL_USERS) {
            String ut = sd.getUserType() == UserType.MEMBERS ? "MEMBER" : "NON_MEMBER";
            drl.append(",\n").append("            userType != null,\n").append("            userType == \"").append(ut).append("\"");
        }

        drl.append("\n        )\n");
        drl.append("    then\n");

        if (sd.getDiscountType() == DiscountType.PERCENTAGE) {
            drl.append("        double discount = " + "$cart.getCartTotal() * ").append(sd.getAmount()).append(" / 100.0;\n");
        } else {
            drl.append("        double discount = Math.min(").append(sd.getAmount()).append(", $cart.getCartTotal());\n");
        }

        boolean needsApproval = Boolean.TRUE.equals(sd.getManagerApprovalRequired());
        boolean isStackable = Boolean.TRUE.equals(sd.getStackable());

        drl.append("        $result.addSystemDiscount(\n").append("            \"").append(ruleName).append("\",\n").append("            \"").append(sd.getDisplayName() != null ? sd.getDisplayName() : sd.getTitle()).append("\",\n").append("            \"").append(sd.getDescription() != null ? sd.getDescription() : "").append("\",\n").append("            ").append(sd.getIcon() != null ? "\"" + sd.getIcon() + "\"" : "null").append(",\n").append("            discount,\n").append("            ").append(isStackable).append(",\n").append("            ").append(needsApproval).append(");\n");

        if (!isStackable) {
            drl.append("        $result.setNonStackableLocked(true);\n");
        }

        if (sd.getStackGroup() != null && !sd.getStackGroup().isBlank()) {
            drl.append("        $result.markStackGroupFired(\"").append(sd.getStackGroup()).append("\");\n");
        }

        drl.append("        update($result);\n");
        drl.append("end\n");

        return drl.toString();
    }

    // ════════════════════════════════════════
    //  DISCOUNT HELPERS
    // ════════════════════════════════════════
    private void appendFullCartDiscount(StringBuilder drl, Promotion p, String ruleName) {
        if (p.getDiscountType() == Promotion.DiscountType.PERCENTAGE) {
            drl.append("        double discount = " + "$cart.getCartTotal() * ").append(p.getAmount()).append(" / 100.0;\n");
        } else {
            drl.append("        double discount = Math.min(").append(p.getAmount()).append(", $cart.getCartTotal());\n");
        }
        drl.append("        $result.addPromoDiscount(\"").append(ruleName).append("\", \"").append(p.getDisplayMessage() != null ? p.getDisplayMessage() : ruleName).append("\", discount);\n");
    }

    private void appendCategoryDiscount(StringBuilder drl, Promotion p, String ruleName) {
        drl.append("        double baseAmount = " + "$cart.getCategoryTotal(\"").append(p.getCategory()).append("\");\n");
        if (p.getDiscountType() == Promotion.DiscountType.PERCENTAGE) {
            drl.append("        double discount = baseAmount * ").append(p.getAmount()).append(" / 100.0;\n");
        } else {
            drl.append("        double discount = Math.min(").append(p.getAmount()).append(", baseAmount);\n");
        }
        drl.append("        $result.addPromoDiscount(\"").append(ruleName).append("\", \"").append(p.getDisplayMessage() != null ? p.getDisplayMessage() : ruleName).append("\", discount);\n");
    }

    private void appendTicketLevelDiscount(StringBuilder drl, Promotion p, String ruleName) {
        drl.append("        double totalDiscount = 0.0;\n");

        Map<String, Boolean> applyAll = p.getApplyAllPerTicketTitle();
        Map<String, Integer> quantities = p.getTicketQuantities();
        List<String> titles = p.getTicketTitles();

        for (String title : titles) {
            boolean allChecked = applyAll != null && Boolean.TRUE.equals(applyAll.get(title));
            Integer qty = quantities != null ? quantities.get(title) : null;

            String safeTitle = title.replace(" ", "_");
            drl.append("        // Ticket: ").append(title).append("\n");

            if (allChecked) {
                drl.append("        double base_").append(safeTitle).append(" = $cart.getTicketTitleTotal(\"").append(title).append("\");\n");
            } else if (qty != null && qty > 0) {
                drl.append("        double base_").append(safeTitle).append(" = $cart.getTicketTitleTotalForQty(\"").append(title).append("\", ").append(qty).append(");\n");
            } else {
                drl.append("        double base_").append(safeTitle).append(" = 0.0;\n");
            }

            if (p.getDiscountType() == Promotion.DiscountType.PERCENTAGE) {
                drl.append("        totalDiscount += base_").append(safeTitle).append(" * ").append(p.getAmount()).append(" / 100.0;\n");
            } else {
                drl.append("        totalDiscount += base_").append(safeTitle).append(" > 0 ? ").append(p.getAmount()).append(" : 0.0;\n");
            }
        }

        drl.append("        $result.addPromoDiscount(\"").append(ruleName).append("\", \"").append(p.getDisplayMessage() != null ? p.getDisplayMessage() : ruleName).append("\", totalDiscount);\n");
    }

    // ════════════════════════════════════════
    //  PRIORITY CONVERTER
    // ════════════════════════════════════════
    private int convertPriority(Integer uiPriority) {
        if (uiPriority == null) return 100;
        return switch (uiPriority) {
            case 1 -> 100;
            case 2 -> 80;
            case 3 -> 60;
            case 4 -> 40;
            case 5 -> 20;
            default -> 60;
        };
    }

    // ════════════════════════════════════════
    //  HISTORY HELPER
    // ════════════════════════════════════════
    private void saveHistory(RuleDefinition rule, String action, String changedBy) {
        RuleHistory h = new RuleHistory();
        h.setRuleName(rule.getRuleName());
        h.setRuleType(rule.getRuleType());
        h.setReferenceId(rule.getReferenceId());
        h.setDrlContent(rule.getDrlContent());
        h.setVersion(rule.getVersion());
        h.setAction(action);
        h.setChangedBy(changedBy != null ? changedBy : "SYSTEM");
        ruleHistoryRepository.save(h);
    }

    private RuleDefinition findOrNew(String ruleType, Long referenceId) {
        return ruleDefinitionRepository.findByRuleTypeAndReferenceIdAndActiveTrue(ruleType, referenceId).orElse(new RuleDefinition());
    }
}