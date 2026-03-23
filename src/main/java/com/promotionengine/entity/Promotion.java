package com.promotionengine.entity;

import com.promotionengine.enums.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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

  @Enumerated(EnumType.STRING)
  private RedemptionMethod redemptionMethod;

  @Enumerated(EnumType.STRING)
  private PromotionType promotionType;

  @Column(unique = true)
  private String promoCode;

  private String displayMessage;

  @Enumerated(EnumType.STRING)
  private DiscountType discountType;

  private Double amount;

  private Boolean stackable;

  private Integer priority;

  // ✅ When true → ticket tables will be EMPTY (correct)
  // When false → ticket tables should have data
  private Boolean applyFullCart;

  private String category;
  private String ticketType;

  // ✅ FIX: @ElementCollection with proper
  // @CollectionTable and column definitions
  // MUST have cascade on parent entity save

  @ElementCollection
  @CollectionTable(
          name = "promotion_ticket_titles",
          joinColumns = @JoinColumn(name = "promotion_id")
  )
  @Column(name = "ticket_title")
  // ✅ Initialize to empty list — never null
  private List<String> ticketTitles = new ArrayList<>();

  @ElementCollection
  @CollectionTable(
          name = "promotion_ticket_quantities",
          joinColumns = @JoinColumn(name = "promotion_id")
  )
  @MapKeyColumn(name = "ticket_title")
  @Column(name = "quantity")
  // ✅ Initialize to empty map — never null
  private Map<String, Integer> ticketQuantities
          = new HashMap<>();

  @ElementCollection
  @CollectionTable(
          name = "promotion_ticket_apply_all",
          joinColumns = @JoinColumn(name = "promotion_id")
  )
  @MapKeyColumn(name = "ticket_title")
  @Column(name = "apply_all")
  // ✅ Initialize to empty map — never null
  private Map<String, Boolean> applyAllPerTicketTitle
          = new HashMap<>();

  private LocalDateTime startDate;
  private LocalDateTime endDate;

  private Boolean channelWeb;
  private Boolean channelPos;

  @Enumerated(EnumType.STRING)
  private UserType userType;

  private Integer usageLimit;
  private Integer maxUsagePerCustomer;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PromotionStatus status;

  private Boolean active = false;

  private String createdBy;
  private String updatedBy;

  @CreationTimestamp
  private LocalDateTime createdAt;

  @UpdateTimestamp
  private LocalDateTime updatedAt;


}