package com.promotionengine.repository;

import com.promotionengine.entity.DiscountAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscountAuditLogRepository extends JpaRepository<DiscountAuditLog, Long> {

    // Audit logs for a customer newest first
    List<DiscountAuditLog> findByCustomerIdOrderByAppliedAtDesc(String customerId);

    // Audit logs for a specific promo code
    List<DiscountAuditLog> findByPromoCodesContainingOrderByAppliedAtDesc(String promoCode);
}