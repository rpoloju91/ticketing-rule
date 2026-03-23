package com.promotionengine.entity;

import com.promotionengine.enums.DiscountStatus;
import com.promotionengine.enums.DiscountType;
import com.promotionengine.enums.UserType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_discount")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Internal name
    private String title;

    // Shown on POS and checkout
    private String displayName;

    private String description;

    // PERCENTAGE or FLAT
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    private Double amount;

    // Icon chosen from UI
    private String icon;

    // DATE DRIVEN
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Manager approval required toggle
    private Boolean managerApprovalRequired;

    // true → can stack with other system discounts
    // false → exclusive, blocks all other system discounts
    private Boolean stackable;

    // ALL_USERS / NON_MEMBERS / MEMBERS
    @Enumerated(EnumType.STRING)
    private UserType userType;

    // DRAFT / PUBLISHED / INACTIVE
    @Enumerated(EnumType.STRING)
    private DiscountStatus status;

    // Only PUBLISHED and active discounts generate DRL rules
    private Boolean active = false;

    // Used as Drools salience
    private Integer priority;

    // Mutual exclusion group for system discounts
    private String stackGroup;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = DiscountStatus.DRAFT;
        if (active == null) active = false;
        if (stackable == null) stackable = true;
        if (managerApprovalRequired == null) managerApprovalRequired = false;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }


}