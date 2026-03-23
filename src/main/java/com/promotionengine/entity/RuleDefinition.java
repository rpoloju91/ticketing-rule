package com.promotionengine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "rule_definitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleDefinition {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // e.g. PROMO_SAVE10, SYSDISCOUNT_LOYALTY
          private String ruleName;

  // PROMOTION or SYSTEM_DISCOUNT
          private String ruleType;

  // FK topromotion.idorsystem_discount.id
          private Long referenceId;

  @Lob
  @Column(columnDefinition = "CLOB")
  private String drlContent;

  private Integer version = 1;

  private boolean active = true;

  private String createdBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (version == null) version = 1;
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}