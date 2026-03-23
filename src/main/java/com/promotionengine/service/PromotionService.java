package com.promotionengine.service;

import com.promotionengine.entity.Promotion;
import com.promotionengine.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionService {
    @Autowired
    PromotionRepository promotionRepository;
    @Autowired
    DroolsRuleService droolsRuleService;

    // ════════════════════════════════════════
    // CREATE
    // status=DRAFT   → save, no rule
    // status=PUBLISHED → save + generate rule
    // ════════════════════════════════════════
    @Transactional
    public Promotion create(Promotion promotion, String createdBy) {
        // Validate duplicate promo code
        if (promotion.getRedemptionMethod() == Promotion.RedemptionMethod.PROMO_CODE && promotion.getPromoCode() != null && promotionRepository.existsByPromoCode(promotion.getPromoCode())) {
            throw new RuntimeException("Promo code already exists: " + promotion.getPromoCode());
        }

        // Default to DRAFT if not provided
        if (promotion.getStatus() == null) {
            promotion.setStatus(Promotion.PromotionStatus.DRAFT);
        }

        promotion.setActive(promotion.getStatus() == Promotion.PromotionStatus.PUBLISHED);
        promotion.setCreatedBy(createdBy);

        Promotion saved = promotionRepository.save(promotion);
        log.info("Promotion created id={} status={}", saved.getId(), saved.getStatus());

        // Generate DRL only if PUBLISHED
        if (saved.getStatus() == Promotion.PromotionStatus.PUBLISHED) {
            droolsRuleService.generatePromotionRule(saved, createdBy);
        }

        return saved;
    }

    // ════════════════════════════════════════
    // UPDATE
    // status drives rule action
    // ════════════════════════════════════════
    @Transactional
    public Promotion update(Long id, Promotion updated, String updatedBy) {

        Promotion existing = getById(id);

        Promotion.PromotionStatus newStatus = updated.getStatus() != null ? updated.getStatus() : existing.getStatus();

        existing.setRedemptionMethod(updated.getRedemptionMethod());
        existing.setPromotionType(updated.getPromotionType());
        existing.setPromoCode(updated.getPromoCode());
        existing.setDisplayMessage(updated.getDisplayMessage());
        existing.setUsageLimit(updated.getUsageLimit());
        existing.setMaxUsagePerCustomer(updated.getMaxUsagePerCustomer());
        existing.setDiscountType(updated.getDiscountType());
        existing.setAmount(updated.getAmount());
        existing.setStackable(updated.getStackable());
        existing.setPriority(updated.getPriority());
        existing.setApplyFullCart(updated.getApplyFullCart());
        existing.setCategory(updated.getCategory());
        existing.setTicketType(updated.getTicketType());
        existing.setTicketTitles(updated.getTicketTitles());
        existing.setTicketQuantities(updated.getTicketQuantities());
        existing.setApplyAllPerTicketTitle(updated.getApplyAllPerTicketTitle());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        existing.setChannelWeb(updated.getChannelWeb());
        existing.setChannelPos(updated.getChannelPos());
        existing.setUserType(updated.getUserType());
        existing.setStatus(newStatus);
        existing.setActive(newStatus == Promotion.PromotionStatus.PUBLISHED);
        existing.setUpdatedBy(updatedBy);

        Promotion saved = promotionRepository.save(existing);
        log.info("Promotion updated id={} status={}", id, newStatus);

        if (newStatus == Promotion.PromotionStatus.PUBLISHED) {
            droolsRuleService.generatePromotionRule(saved, updatedBy);
        } else if (newStatus == Promotion.PromotionStatus.INACTIVE) {
            droolsRuleService.deactivateRule("PROMOTION", id, updatedBy);
        }

        return saved;
    }

    // ════════════════════════════════════════
    // GET ALL
    // ════════════════════════════════════════
    public List<Promotion> getAll() {
        return promotionRepository.findAll();
    }

    // ════════════════════════════════════════
    // GET BY STATUS
    // ════════════════════════════════════════
    public List<Promotion> getByStatus(Promotion.PromotionStatus status) {
        return promotionRepository.findByStatus(status);
    }

    // ════════════════════════════════════════
    // GET BY ID
    // ════════════════════════════════════════
    public Promotion getById(Long id) {
        return promotionRepository.findById(id).orElseThrow(() -> new RuntimeException("Promotion not found: " + id));
    }
}