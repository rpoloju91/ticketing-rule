package com.promotionengine.scheduler;

import com.promotionengine.entity.Promotion;
import com.promotionengine.entity.SystemDiscount;
import com.promotionengine.enums.DiscountStatus;
import com.promotionengine.repository.PromotionRepository;
import com.promotionengine.repository.RuleDefinitionRepository;
import com.promotionengine.repository.SystemDiscountRepository;
import com.promotionengine.service.DroolsEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscountScheduler {

    private final PromotionRepository promotionRepository;
    private final SystemDiscountRepository systemDiscountRepository;
    private final RuleDefinitionRepository ruleDefinitionRepository;
    private final DroolsEngineService droolsEngineService;

    // Every hour — expire promotions past end date
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expirePromotions() {
        LocalDateTime now = LocalDateTime.now();
        List<Promotion> expired = promotionRepository.findByActiveTrueAndEndDateBefore(now);

        if (!expired.isEmpty()) {
            expired.forEach(p -> {
                p.setActive(false);
                p.setStatus(Promotion.PromotionStatus.INACTIVE);
                log.info("Expiring promo: {}", p.getPromoCode());
            });
            promotionRepository.saveAll(expired);

            expired.forEach(p -> ruleDefinitionRepository.findByRuleTypeAndReferenceIdAndActiveTrue("PROMOTION", p.getId()).ifPresent(rule -> {
                rule.setActive(false);
                ruleDefinitionRepository.save(rule);
            }));

            droolsEngineService.reloadRules();
            log.info("Expired {} promotions", expired.size());
        }
    }

    // Every hour — expire system discounts past end date
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireSystemDiscounts() {
        LocalDateTime now = LocalDateTime.now();
        List<SystemDiscount> expired = systemDiscountRepository.findByActiveTrueAndEndDateBefore(now);

        if (!expired.isEmpty()) {
            expired.forEach(sd -> {
                sd.setActive(false);
                sd.setStatus(DiscountStatus.INACTIVE);
                log.info("Expiring discount: {}", sd.getTitle());
            });
            systemDiscountRepository.saveAll(expired);

            expired.forEach(sd -> ruleDefinitionRepository.findByRuleTypeAndReferenceIdAndActiveTrue("SYSTEM_DISCOUNT", sd.getId()).ifPresent(rule -> {
                rule.setActive(false);
                ruleDefinitionRepository.save(rule);
            }));

            droolsEngineService.reloadRules();
            log.info("Expired {} system discounts", expired.size());
        }
    }

    // Midnight — activate scheduled promotions
    // whose startDate has arrived
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void activateScheduledPromotions() {
        LocalDateTime now = LocalDateTime.now();
        List<Promotion> toActivate = promotionRepository.findByActiveFalseAndStatusAndStartDateBeforeAndEndDateAfter(Promotion.PromotionStatus.DRAFT, now, now);

        if (!toActivate.isEmpty()) {
            toActivate.forEach(p -> {
                p.setActive(true);
                p.setStatus(Promotion.PromotionStatus.PUBLISHED);
                log.info("Activating promo: {}", p.getPromoCode());
            });
            promotionRepository.saveAll(toActivate);
            droolsEngineService.reloadRules();
            log.info("Activated {} promotions", toActivate.size());
        }
    }

    // Midnight — activate scheduled system discounts
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void activateScheduledSystemDiscounts() {
        LocalDateTime now = LocalDateTime.now();
        List<SystemDiscount> toActivate = systemDiscountRepository.findByActiveFalseAndStatusAndStartDateBeforeAndEndDateAfter(DiscountStatus.DRAFT, now, now);

        if (!toActivate.isEmpty()) {
            toActivate.forEach(sd -> {
                sd.setActive(true);
                sd.setStatus(DiscountStatus.PUBLISHED);
                log.info("Activating discount: {}", sd.getTitle());
            });
            systemDiscountRepository.saveAll(toActivate);
            droolsEngineService.reloadRules();
            log.info("Activated {} system discounts", toActivate.size());
        }
    }
}