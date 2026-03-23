package com.promotionengine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "promo_usage",
  uniqueConstraints = @UniqueConstraint(
    columnNames = {"promo_code", "customer_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromoUsage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "promo_code")
  private String promoCode;

  // Per customer tracking
          @Column(name = "customer_id")
  private String customerId;

  // How many times this customer used this promo
          private Integer usageCount = 0;

  private LocalDateTime firstUsedAt;
  private LocalDateTime lastUsedAt;

  @PrePersist
  public void prePersist() {
    firstUsedAt = LocalDateTime.now();
    lastUsedAt = LocalDateTime.now();
    if (usageCount == null) usageCount = 0;
  }

  @PreUpdate
  public void preUpdate() {
    lastUsedAt = LocalDateTime.now();
  }
}