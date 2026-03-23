package com.promotionengine.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class DiscountResult {

  private Double originalTotal  = 0.0;
  private Double totalDiscount  = 0.0;
  private Double finalTotal     = 0.0;

  private boolean promoStackLocked    = false;
  private boolean nonStackableLocked  = false;

  private Set<String> firedStackGroups = new HashSet<>();

  // ✅ KEY FIX: Track which rules already fired
  // Drools checks this in WHEN block
  // If rule already fired → condition false → no re-fire
  private Set<String> firedRuleNames  = new HashSet<>();

  private List<AppliedDiscount> appliedDiscounts
          = new ArrayList<>();

  private boolean managerApprovalRequired = false;

  // ✅ FIX: Check before adding — idempotent
  public void addPromoDiscount(
          String ruleName,
          String displayMessage,
          Double amount) {

    // ✅ Guard: if already fired, do nothing
    if (firedRuleNames.contains(ruleName)) return;
    if (amount == null || amount <= 0) return;

    firedRuleNames.add(ruleName);   // ← mark fired

    AppliedDiscount ad = new AppliedDiscount();
    ad.setRuleName(ruleName);
    ad.setDisplayMessage(displayMessage);
    ad.setDiscountAmount(amount);
    ad.setDiscountType("PROMOTION");
    ad.setStackable(true);
    ad.setManagerApprovalRequired(false);
    appliedDiscounts.add(ad);
    totalDiscount += amount;
  }

  // ✅ FIX: Check before adding — idempotent
  public void addSystemDiscount(
          String ruleName,
          String displayName,
          String description,
          String icon,
          Double amount,
          boolean stackable,
          boolean managerApproval) {

    // ✅ Guard: if already fired, do nothing
    if (firedRuleNames.contains(ruleName)) return;
    if (amount == null || amount <= 0) return;

    firedRuleNames.add(ruleName);   // ← mark fired

    AppliedDiscount ad = new AppliedDiscount();
    ad.setRuleName(ruleName);
    ad.setDisplayMessage(displayName);
    ad.setDescription(description);
    ad.setIcon(icon);
    ad.setDiscountAmount(amount);
    ad.setDiscountType("SYSTEM_DISCOUNT");
    ad.setStackable(stackable);
    ad.setManagerApprovalRequired(managerApproval);
    appliedDiscounts.add(ad);
    totalDiscount  += amount;

    if (managerApproval) {
      this.managerApprovalRequired = true;
    }
  }

  public boolean isRuleAlreadyFired(String ruleName) {
    return firedRuleNames.contains(ruleName);
  }

  public boolean isStackGroupAlreadyFired(
          String stackGroup) {
    if (stackGroup == null
            || stackGroup.isEmpty()) return false;
    return firedStackGroups.contains(stackGroup);
  }

  public void markStackGroupFired(String stackGroup) {
    if (stackGroup != null
            && !stackGroup.isEmpty()) {
      firedStackGroups.add(stackGroup);
    }
  }

  // ✅ Ensure finalTotal never goes negative
  public void computeFinalTotal() {
    finalTotal = Math.max(0.0,
            (originalTotal != null ? originalTotal : 0.0)
                    - (totalDiscount != null ? totalDiscount : 0.0));
  }

  @Data
  public static class AppliedDiscount {
    private String  ruleName;
    private String  displayMessage;
    private String  description;
    private String  icon;
    private Double  discountAmount;
    private String  discountType;
    private boolean stackable;
    private boolean managerApprovalRequired;
  }
}