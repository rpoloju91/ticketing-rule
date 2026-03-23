package com.promotionengine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "rule_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String ruleName;
  private String ruleType;
  private Long referenceId;

  @Lob
  @Column(columnDefinition = "CLOB")
  private String drlContent;

  private Integer version;

  // CREATED / UPDATED / DEACTIVATED /
          // ROLLED_BACK / ROLLBACK_TO_Vx
          private String action;

  private String changedBy;
  private LocalDateTime changedAt;

  @PrePersist
  public void prePersist() {
    changedAt = LocalDateTime.now();
  }
}