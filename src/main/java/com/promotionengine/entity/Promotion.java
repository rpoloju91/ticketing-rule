package com.promotionengine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "promotion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // PROMO_CODE → customer types code
          // AUTOMATIC → system auto applies
          @Enumerated(EnumType.STRING)
  private RedemptionMethod redemptionMethod;

  // OFF_PRODUCT / PERCENTAGE / FLAT
          @Enumerated(EnumType.STRING)
  private PromotionType promotionType;

  private String promoCode;
  private String displayMessage;

  // Global usage limit across all customers
          private Integer usageLimit;

  // Per customer max usage
          private Integer maxUsagePerCustomer;

  // PERCENTAGE or FLAT
          @Enumerated(EnumType.STRING)
  private DiscountType discountType;
  private Double amount;

  // Admin decides stackable per promotion
          // true → can combine with other stackable promos
          // false → exclusive, cannot combine with any other promo
          private Boolean stackable;

  // 1=Highest priority, 5=Lowest priority (slider in UI)
          private Integer priority;

  // true → apply on full cart total
          // false → apply on category/ticket level
          private Boolean applyFullCart;

  // Used when applyFullCart = false
          private String category;
  private String ticketType;

  @ElementCollection
  @CollectionTable(
    name = "promotion_ticket_titles",
                joinColumns = @JoinColumn(name = "promotion_id"))
              @Column(name = "ticket_title")
  private List<String> ticketTitles;

  @ElementCollection
  @CollectionTable(
    name = "promotion_ticket_quantities",
                joinColumns = @JoinColumn(name = "promotion_id"))
              @MapKeyColumn(name = "ticket_title")
  @Column(name = "quantity")
  private Map<String, Integer> ticketQuantities;

  @ElementCollection
  @CollectionTable(
    name = "promotion_ticket_apply_all",
                joinColumns = @JoinColumn(name = "promotion_id"))
              @MapKeyColumn(name = "ticket_title")
  @Column(name = "apply_all")
  private Map<String, Boolean> applyAllPerTicketTitle;

  // DATE DRIVEN
          private LocalDateTime startDate;
  private LocalDateTime endDate;

  // Sales & distribution channels
          private Boolean channelWeb;
  private Boolean channelPos;

  // ALL_USERS / NON_MEMBERS / MEMBERS
          @Enumerated(EnumType.STRING)
  private UserType userType;

  // DRAFT / PUBLISHED / INACTIVE
          @Enumerated(EnumType.STRING)
  private PromotionStatus status;

  private Boolean active = false;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String createdBy;
  private String updatedBy;

  @PrePersist
  public void prePersist() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (status == null) status = PromotionStatus.DRAFT;
    if (active == null) active = false;
    if (stackable == null) stackable = true;
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public enum RedemptionMethod {
    PROMO_CODE, AUTOMATIC
  }

  public enum PromotionType {
    OFF_PRODUCT, PERCENTAGE, FLAT
  }

  public enum DiscountType {
    PERCENTAGE, FLAT
  }

  public enum UserType {
    ALL_USERS, NON_MEMBERS, MEMBERS
  }

  public enum PromotionStatus {
    DRAFT, PUBLISHED, INACTIVE
  }
}