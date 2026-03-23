package com.promotionengine.service;

import com.promotionengine.dto.PromotionRequest;
import com.promotionengine.entity.Promotion;
import com.promotionengine.enums.PromotionStatus;
import com.promotionengine.enums.RedemptionMethod;
import com.promotionengine.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
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
    public Promotion create(PromotionRequest req, String createdBy) {
        // Duplicate check
        promotionRepository
                .findByPromoCode(req.getPromoCode())
                .ifPresent(p -> {
                    throw new RuntimeException(
                            "Promo code already exists: "
                                    + req.getPromoCode());
                });

        Promotion p = new Promotion();
        mapRequestToEntity(req, p);
        p.setCreatedBy(createdBy);

        // Set active based on status
        p.setActive(
                req.getStatus() ==
                        PromotionStatus.PUBLISHED);

        Promotion saved =
                promotionRepository.save(p);

        log.info("Saved promotion id={} promoCode={} "
                        + "ticketTitles={} ticketQty={} applyAll={}",
                saved.getId(),
                saved.getPromoCode(),
                saved.getTicketTitles(),       // ← log it
                saved.getTicketQuantities(),   // ← log it
                saved.getApplyAllPerTicketTitle()); // ← log it

        // Generate DRL only if PUBLISHED
        if (req.getStatus() ==
                PromotionStatus.PUBLISHED) {
            droolsRuleService.generatePromotionRule(
                    saved, createdBy);
        }

        return saved;
    }

    // ════════════════════════════════════════
    // UPDATE
    // status drives rule action
    // ════════════════════════════════════════
    @Transactional
    public Promotion update(Long id, PromotionRequest req, String updatedBy) {

        Promotion p = promotionRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Promotion not found: " + id));

        // ✅ FIX: Clear old collections before setting new
        // Without this old data stays + new data appended
        p.getTicketTitles().clear();
        p.getTicketQuantities().clear();
        p.getApplyAllPerTicketTitle().clear();

        mapRequestToEntity(req, p);
        p.setUpdatedBy(updatedBy);
        p.setActive(
                req.getStatus() ==
                        PromotionStatus.PUBLISHED);

        Promotion saved =
                promotionRepository.save(p);

        if (req.getStatus() ==
                PromotionStatus.PUBLISHED) {
            droolsRuleService.generatePromotionRule(
                    saved, updatedBy);
        } else if (req.getStatus() ==
                PromotionStatus.INACTIVE) {
            droolsRuleService.deactivateRule(
                    "PROMOTION", id, updatedBy);
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
    public List<Promotion> getByStatus(PromotionStatus status) {
        return promotionRepository.findByStatus(status);
    }

    // ════════════════════════════════════════
    // GET BY ID
    // ════════════════════════════════════════
    public Promotion getById(Long id) {
        return promotionRepository.findById(id).orElseThrow(() -> new RuntimeException("Promotion not found: " + id));
    }

    // ════════════════════════════════════════
    //  DTO → ENTITY MAPPING
    // ════════════════════════════════════════
    private void mapRequestToEntity(
            PromotionRequest req, Promotion p) {

        p.setRedemptionMethod(req.getRedemptionMethod());
        p.setPromotionType(req.getPromotionType());
        p.setPromoCode(req.getPromoCode());
        p.setDisplayMessage(req.getDisplayMessage());
        p.setDiscountType(req.getDiscountType());
        p.setAmount(req.getAmount());
        p.setStackable(req.getStackable());
        p.setPriority(req.getPriority());
        p.setApplyFullCart(req.getApplyFullCart());
        p.setCategory(req.getCategory());
        p.setTicketType(req.getTicketType());
        p.setStartDate(req.getStartDate());
        p.setEndDate(req.getEndDate());
        p.setChannelWeb(req.getChannelWeb());
        p.setChannelPos(req.getChannelPos());
        p.setUserType(req.getUserType());
        p.setStatus(req.getStatus());
        p.setUsageLimit(req.getUsageLimit());
        p.setMaxUsagePerCustomer(
                req.getMaxUsagePerCustomer());

        // ✅ KEY FIX: Only populate ticket tables
        // when applyFullCart is FALSE
        if (!Boolean.TRUE.equals(req.getApplyFullCart())) {

            // ✅ FIX: Null safe copy of ticketTitles
            if (req.getTicketTitles() != null
                    && !req.getTicketTitles().isEmpty()) {
                p.setTicketTitles(
                        new ArrayList<>(
                                req.getTicketTitles()));
            }

            // ✅ FIX: Null safe copy of ticketQuantities
            if (req.getTicketQuantities() != null
                    && !req.getTicketQuantities()
                    .isEmpty()) {
                p.setTicketQuantities(
                        new HashMap<>(
                                req.getTicketQuantities()));
            }

            // ✅ FIX: Null safe copy of applyAll
            if (req.getApplyAllPerTicketTitle() != null
                    && !req.getApplyAllPerTicketTitle()
                    .isEmpty()) {
                p.setApplyAllPerTicketTitle(
                        new HashMap<>(
                                req.getApplyAllPerTicketTitle()));
            }
        } else {
            // ✅ When fullCart = true, clear ticket fields
            p.setTicketTitles(new ArrayList<>());
            p.setTicketQuantities(new HashMap<>());
            p.setApplyAllPerTicketTitle(new HashMap<>());
            p.setCategory(null);
            p.setTicketType(null);
        }
    }
}