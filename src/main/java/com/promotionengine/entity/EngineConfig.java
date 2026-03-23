package com.promotionengine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "engine_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EngineConfig {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String configKey;

  @Column(nullable = false)
  private String configValue;

  private String description;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String updatedBy;

  @PrePersist
  public void prePersist() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // Config key constants
          public static final String MAX_PROMOS_PER_CART
    = "MAX_PROMOS_PER_CART";
}