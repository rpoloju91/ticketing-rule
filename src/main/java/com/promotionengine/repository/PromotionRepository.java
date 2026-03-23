package com.promotionengine.repository;

import com.promotionengine.entity.Promotion;
import com.promotionengine.enums.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

  // Find active promo by code
          Optional<Promotion> findByPromoCodeAndActiveTrue(
      String promoCode);

  // Find by status
          List<Promotion> findByStatus(
      PromotionStatus status);

  // Scheduler: find expired active promotions
          List<Promotion> findByActiveTrueAndEndDateBefore(
      LocalDateTime now);

  // Scheduler: find scheduled promos to activate
          List<Promotion>
  findByActiveFalseAndStatusAndStartDateBeforeAndEndDateAfter(
      PromotionStatus status,
      LocalDateTime start,
      LocalDateTime end);

  // Duplicate check
          boolean existsByPromoCode(String promoCode);

    Optional<Object> findByPromoCode(String promoCode);
}