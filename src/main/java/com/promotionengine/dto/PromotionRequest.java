package com.promotionengine.dto;

import com.promotionengine.entity.Promotion;
import com.promotionengine.enums.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class PromotionRequest {

    private RedemptionMethod redemptionMethod;
    private PromotionType promotionType;
    private String promoCode;
    private String displayMessage;
    private DiscountType discountType;
    private Double amount;
    private Boolean stackable;
    private Integer priority;
    private Boolean applyFullCart;
    private String category;
    private String ticketType;

    // ✅ Field names MUST exactly match what
    // the request JSON sends
    // ticketTitles       → ["Adult", "Youth", "Child"]
    // ticketQuantities   → {"Adult": 2, "Youth": 0}
    // applyAllPerTicketTitle → {"Adult": false, "Youth": true}

    private List<String> ticketTitles = new ArrayList<>();

    private Map<String, Integer> ticketQuantities = new HashMap<>();

    private Map<String, Boolean> applyAllPerTicketTitle = new HashMap<>();

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Boolean channelWeb;
    private Boolean channelPos;

    private UserType userType;
    private PromotionStatus status;

    private Integer usageLimit;
    private Integer maxUsagePerCustomer;
}