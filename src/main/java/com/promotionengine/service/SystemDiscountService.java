package com.promotionengine.service;

import com.promotionengine.entity.SystemDiscount;
import com.promotionengine.enums.DiscountStatus;
import com.promotionengine.repository.SystemDiscountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemDiscountService {
    @Autowired
    SystemDiscountRepository systemDiscountRepository;
    @Autowired
    DroolsRuleService droolsRuleService;

    // ════════════════════════════════════════
    // CREATE
    // status=DRAFT   → save, no rule
    // status=PUBLISHED → save + generate rule
    // ════════════════════════════════════════
    @Transactional
    public SystemDiscount create(SystemDiscount discount, String createdBy) {

        if (discount.getStatus() == null) {
            discount.setStatus(DiscountStatus.DRAFT);
        }

        discount.setActive(discount.getStatus() == DiscountStatus.PUBLISHED);
        discount.setCreatedBy(createdBy);

        SystemDiscount saved = systemDiscountRepository.save(discount);
   log.info("SystemDiscount created id={} status={}", saved.getId(), saved.getStatus());

        if (saved.getStatus() == DiscountStatus.PUBLISHED) {
            droolsRuleService.generateSystemDiscountRule(saved, createdBy);
        }

        return saved;
    }

    // ════════════════════════════════════════
    // UPDATE
    // ════════════════════════════════════════
    @Transactional
    public SystemDiscount update(Long id, SystemDiscount updated, String updatedBy) {

        SystemDiscount existing = getById(id);

        DiscountStatus newStatus = updated.getStatus() != null ? updated.getStatus() : existing.getStatus();

        existing.setTitle(updated.getTitle());
        existing.setDisplayName(updated.getDisplayName());
        existing.setDescription(updated.getDescription());
        existing.setDiscountType(updated.getDiscountType());
        existing.setAmount(updated.getAmount());
        existing.setIcon(updated.getIcon());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        existing.setManagerApprovalRequired(updated.getManagerApprovalRequired());
        existing.setStackable(updated.getStackable());
        existing.setUserType(updated.getUserType());
        existing.setPriority(updated.getPriority());
        existing.setStackGroup(updated.getStackGroup());
        existing.setStatus(newStatus);
        existing.setActive(newStatus == DiscountStatus.PUBLISHED);
        existing.setUpdatedBy(updatedBy);

        SystemDiscount saved = systemDiscountRepository.save(existing);
   log.info("SystemDiscount updated id={} status={}", id, newStatus);

        if (newStatus == DiscountStatus.PUBLISHED) {
            droolsRuleService.generateSystemDiscountRule(saved, updatedBy);
        } else if (newStatus == DiscountStatus.INACTIVE) {
            droolsRuleService.deactivateRule("SYSTEM_DISCOUNT", id, updatedBy);
        }

        return saved;
    }

    // ════════════════════════════════════════
    // GET ALL
    // ════════════════════════════════════════
    public List<SystemDiscount> getAll() {
        return systemDiscountRepository.findAll();
    }

    // ════════════════════════════════════════
    // GET BY STATUS
    // ════════════════════════════════════════
    public List<SystemDiscount> getByStatus(DiscountStatus status) {
        return systemDiscountRepository.findByStatus(status);
    }

    // ════════════════════════════════════════
    // GET BY ID
    // ════════════════════════════════════════
    public SystemDiscount getById(Long id) {
        return systemDiscountRepository.findById(id).orElseThrow(() -> new RuntimeException("SystemDiscount not found: " + id));
    }
}