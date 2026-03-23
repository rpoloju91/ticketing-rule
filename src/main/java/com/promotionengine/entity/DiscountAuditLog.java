package com.promotionengine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "discount_audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountAuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String customerId;

  // Comma separated applied promo codes
          private String promoCodes;

  private Double originalTotal;
  private Double totalDiscount;
  private Double finalTotal;

  private boolean managerApprovalRequired;
  private int rulesFired;

  // JSON array of applied discounts
          @Lob
  @Column(columnDefinition = "CLOB")
  private String appliedDiscountsJson;

  private LocalDateTime appliedAt;

  @PrePersist
  public void prePersist() {
    appliedAt = LocalDateTime.now();
  }
}