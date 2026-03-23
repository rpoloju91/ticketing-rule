package com.promotionengine.repository;

import com.promotionengine.entity.Promotion;
import org.springframework.data.jpa.domain.Specification;

public class PromotionSpecification {

    public static Specification<Promotion> activeAndPromoCode(String promoCode) {

        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("active")),
                cb.equal(root.get("promoCode"), promoCode)
        );
    }
}