package com.promotionengine.repository;

import com.promotionengine.entity.SystemDiscount;
import com.promotionengine.enums.DiscountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemDiscountRepository extends JpaRepository<SystemDiscount, Long> {

    // Find by status
    List<SystemDiscount> findByStatus(DiscountStatus status);

    // Scheduler: find expired active discounts
    List<SystemDiscount> findByActiveTrueAndEndDateBefore(LocalDateTime now);

    // Scheduler: find scheduled discounts to activate
    List<SystemDiscount> findByActiveFalseAndStatusAndStartDateBeforeAndEndDateAfter(DiscountStatus status, LocalDateTime start, LocalDateTime end);
}