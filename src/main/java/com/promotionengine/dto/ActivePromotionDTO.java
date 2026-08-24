package com.app.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ActivePromotionDTO(
    Long id,
    String name,
    String description,
    String discountType,
    BigDecimal discountValue,
    Long amenityId,
    String amenityName,
    String locationFunctionName,
    LocalDateTime startDateUtc,
    LocalDateTime endDateUtc,
    String locationTimezone,
    LocalDateTime localStart,
    LocalDateTime localEnd,
    LocalDateTime localNow
) {}
