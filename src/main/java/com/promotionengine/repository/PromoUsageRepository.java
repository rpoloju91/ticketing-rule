package com.promotionengine.repository;

import com.promotionengine.entity.PromoUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromoUsageRepository extends JpaRepository<PromoUsage, Long> {

  // Per customer usage record
          Optional<PromoUsage> findByPromoCodeAndCustomerId(
      String promoCode, String customerId);

  // Global usage across all customers
          @Query("SELECT COALESCE(SUM(u.usageCount), 0) " +
     "FROM PromoUsage u " +
             "WHERE u.promoCode = :promoCode")
          Integer getTotalUsageByPromoCode(String promoCode);

  // Increment per customer usage
          @Modifying
  @Query("UPDATE PromoUsage u " +
     "SET u.usageCount = u.usageCount + 1, " +
             "u.lastUsedAt = CURRENT_TIMESTAMP " +
             "WHERE u.promoCode = :promoCode " +
             "AND u.customerId = :customerId")
          int incrementUsage(String promoCode,
           String customerId);
}