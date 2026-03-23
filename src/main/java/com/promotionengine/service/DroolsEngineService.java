package com.promotionengine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promotionengine.dto.Cart;
import com.promotionengine.dto.DiscountResult;
import com.promotionengine.entity.DiscountAuditLog;
import com.promotionengine.entity.PromoUsage;
import com.promotionengine.entity.Promotion;
import com.promotionengine.entity.RuleDefinition;
import com.promotionengine.repository.DiscountAuditLogRepository;
import com.promotionengine.repository.PromoUsageRepository;
import com.promotionengine.repository.PromotionRepository;
import com.promotionengine.repository.RuleDefinitionRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DroolsEngineService {

    @Autowired
    RuleDefinitionRepository ruleDefinitionRepository;
    @Autowired
    PromotionRepository promotionRepository;
    @Autowired
    PromoUsageRepository promoUsageRepository;
    @Autowired
    DiscountAuditLogRepository auditLogRepository;
    @Autowired
    EngineConfigService engineConfigService;
    ObjectMapper objectMapper =new ObjectMapper();

    private final AtomicReference<KieContainer> kieContainerRef = new AtomicReference<>();
    private final AtomicInteger activeSessions = new AtomicInteger(0);

    // ════════════════════════════════════════
    //  STARTUP — load existing rules from DB
    // ════════════════════════════════════════
    @PostConstruct
    public void init() {
        try {
            reloadRules();
        } catch (Exception e) {
            log.warn("Startup rule load — no rules yet " + "or compile warning: {}", e.getMessage());
            buildEmptyContainer();
        }
    }

    // ════════════════════════════════════════
    //  RELOAD RULES FROM DB
    // ════════════════════════════════════════
    public synchronized void reloadRules() {
        List<RuleDefinition> activeRules = ruleDefinitionRepository.findByActiveTrue();

        log.info("Reloading {} active rules...", activeRules.size());

        //  If no rules, build empty container
        // instead of crashing
        if (activeRules.isEmpty()) {
            buildEmptyContainer();
            return;
        }

        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();

        for (RuleDefinition rule : activeRules) {
            String path = "src/main/resources/rules/" + rule.getRuleName() + ".drl";
            kfs.write(path, ks.getResources().newReaderResource(new StringReader(rule.getDrlContent())).setResourceType(org.kie.api.io.ResourceType.DRL));
        }

        KieBuilder kb = ks.newKieBuilder(kfs).buildAll();

        if (kb.getResults().hasMessages(Message.Level.ERROR)) {
            String errors = kb.getResults().getMessages(Message.Level.ERROR).stream().map(Message::getText).collect(Collectors.joining(", "));
            log.error("DRL compile errors: {}", errors);
            throw new RuntimeException("DRL compile error: " + errors);
        }

        KieContainer newContainer = ks.newKieContainer(ks.getRepository().getDefaultReleaseId());

        KieContainer old = kieContainerRef.getAndSet(newContainer);
        if (old != null) {
            disposeOldContainerSafely(old);
        }

        log.info("✅ KieContainer reloaded — {} rules", activeRules.size());
    }

    // ✅ FIX: Build empty container when no rules exist
    private void buildEmptyContainer() {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        // Write a dummy empty rule file
        kfs.write("src/main/resources/rules/empty.drl", ks.getResources().newReaderResource(new StringReader("package com.promotionengine.rules;\n")).setResourceType(org.kie.api.io.ResourceType.DRL));

        KieBuilder kb = ks.newKieBuilder(kfs).buildAll();
        KieContainer container = ks.newKieContainer(ks.getRepository().getDefaultReleaseId());
        KieContainer old = kieContainerRef.getAndSet(container);
        if (old != null) {
            disposeOldContainerSafely(old);
        }
        log.info("Empty KieContainer initialized");
    }

    // ════════════════════════════════════════
    //  APPLY RULES — MAIN ENTRY POINT
    // ════════════════════════════════════════
    @Transactional
    public DiscountResult applyRules(Cart cart) {

        // ✅ Set currentDateTime on cart
        // so DRL date conditions work correctly
        cart.setCurrentDateTime(LocalDateTime.now());

        // STEP 1: Validate promo codes
        if (cart.getPromoCodes() != null && !cart.getPromoCodes().isEmpty()) {
            validatePromoCodes(cart.getPromoCodes(), cart.getCustomerId());
        }

        // STEP 2: Fire Drools
        DiscountResult result = fireRules(cart);

        // STEP 3: Increment usage
        if (cart.getPromoCodes() != null) {
            cart.getPromoCodes().forEach(code -> {
                boolean applied = result.getAppliedDiscounts().stream().anyMatch(d -> d.getRuleName().equals("PROMO_" + code));
                if (applied) {
                    incrementUsage(code, cart.getCustomerId());
                }
            });
        }

        // STEP 4: Audit log async
        saveAuditLogAsync(cart, result);

        return result;
    }

    // ════════════════════════════════════════
    //  VALIDATE PROMO CODES
    // ════════════════════════════════��═══════
    private void validatePromoCodes(List<String> promoCodes, String customerId) {

        // CHECK 1: Max per cart from config
        int maxAllowed = engineConfigService.getMaxPromosPerCart();
        if (promoCodes.size() > maxAllowed) {
            throw new RuntimeException("You can select maximum " + maxAllowed + " promotions per cart. You selected: " + promoCodes.size());
        }

        // CHECK 2: No duplicates
        long distinct = promoCodes.stream().distinct().count();
        if (distinct < promoCodes.size()) {
            throw new RuntimeException("Duplicate promo codes are not allowed.");
        }

        // CHECK 3: Fetch all promos
        List<Promotion> selectedPromos = promoCodes.stream().map(code -> promotionRepository.findByPromoCodeAndActiveTrue(code).orElseThrow(() -> new RuntimeException("Promo not found or " + "inactive: " + code))).collect(Collectors.toList());

        // CHECK 4: Stackable validation
        // If more than 1 selected ALL must be stackable
        if (promoCodes.size() > 1) {
            selectedPromos.forEach(promo -> {
                if (!Boolean.TRUE.equals(promo.getStackable())) {
                    throw new RuntimeException("Promo [" + promo.getPromoCode() + "] is not stackable. " + "Please select it alone.");
                }
            });
        }

        // CHECK 5: Date and usage per promo
        selectedPromos.forEach(promo -> validateDateAndUsage(promo, customerId));
    }

    // ════════════════════════════════════════
    //  VALIDATE DATE AND USAGE
    // ═══��════════════════════════════════════
    private void validateDateAndUsage(Promotion promo, String customerId) {

        String promoCode = promo.getPromoCode();
        LocalDateTime now = LocalDateTime.now();

        if (promo.getStartDate() != null && now.isBefore(promo.getStartDate())) {
            throw new RuntimeException("Promo [" + promoCode + "] not yet active. Starts: " + promo.getStartDate());
        }

        if (promo.getEndDate() != null && now.isAfter(promo.getEndDate())) {
            throw new RuntimeException("Promo [" + promoCode + "] expired on: " + promo.getEndDate());
        }

        if (promo.getUsageLimit() != null) {
            Integer globalUsage = promoUsageRepository.getTotalUsageByPromoCode(promoCode);
            if (globalUsage != null && globalUsage >= promo.getUsageLimit()) {
                throw new RuntimeException("Promo [" + promoCode + "] reached global limit of " + promo.getUsageLimit());
            }
        }

        if (promo.getMaxUsagePerCustomer() != null) {
            int usedCount = promoUsageRepository.findByPromoCodeAndCustomerId(promoCode, customerId).map(PromoUsage::getUsageCount).orElse(0);
            if (usedCount >= promo.getMaxUsagePerCustomer()) {
                throw new RuntimeException("You have already used promo [" + promoCode + "] " + usedCount + " time(s). Max allowed: " + promo.getMaxUsagePerCustomer());
            }
        }
    }

    // ════════════════════════════════════════
    //  FIRE RULES
    // ════════════════════════════════════════
    private DiscountResult fireRules(Cart cart) {
        KieContainer container = kieContainerRef.get();

        // ✅ FIX: Reload if container is null
        if (container == null) {
            log.warn("KieContainer is null — reloading...");
            reloadRules();
            container = kieContainerRef.get();
        }

        // ✅ FIX: Create a fresh DiscountResult
        // with correct initial state
        DiscountResult result = new DiscountResult();
        result.setOriginalTotal(cart.getCartTotal());
        result.setTotalDiscount(0.0);
        result.setPromoStackLocked(false);
        result.setNonStackableLocked(false);

        KieSession kieSession = container.newKieSession();
        activeSessions.incrementAndGet();

        try {
            // ✅ FIX: Insert objects in correct order
            kieSession.insert(cart);
            kieSession.insert(result);

            // ✅ FIX: Use fireAllRules and get count
            int fired = kieSession.fireAllRules();
            log.info("fireAllRules() fired={} " + "customer={} originalTotal={}", fired, cart.getCustomerId(), cart.getCartTotal());

            // ✅ FIX: Compute final total AFTER rules fire
            result.computeFinalTotal();

            log.info("Result: original={} discount={} " + "final={}", result.getOriginalTotal(), result.getTotalDiscount(), result.getFinalTotal());

        } catch (Exception e) {
            log.error("Drools fireAllRules error: {}", e.getMessage(), e);
            // ✅ FIX: Return original total on error
            // instead of crashing
            result.setFinalTotal(cart.getCartTotal());
            result.setTotalDiscount(0.0);
            throw new RuntimeException("Rule engine error: " + e.getMessage());
        } finally {
            // ✅ FIX: Always dispose session
            kieSession.dispose();
            activeSessions.decrementAndGet();
        }

        return result;
    }

    // ════════════════════════════════════════
    //  INCREMENT USAGE
    // ════════════════════════════════════════
    @Transactional
    public void incrementUsage(String promoCode, String customerId) {
        try {
            int updated = promoUsageRepository.incrementUsage(promoCode, customerId);
            if (updated == 0) {
                PromoUsage usage = new PromoUsage();
                usage.setPromoCode(promoCode);
                usage.setCustomerId(customerId);
                usage.setUsageCount(1);
                promoUsageRepository.save(usage);
            }
            log.info("Usage incremented promo={} customer={}", promoCode, customerId);
        } catch (Exception e) {
            log.error("Usage increment failed: {}", e.getMessage());
        }
    }

    // ════════════════════════════════════════
    //  AUDIT LOG ASYNC
    // ════════════════════════════════════════
    @Async
    public void saveAuditLogAsync(Cart cart, DiscountResult result) {
        try {
            DiscountAuditLog log = new DiscountAuditLog();
            log.setCustomerId(cart.getCustomerId());
            log.setPromoCodes(cart.getPromoCodes() != null ? String.join(",", cart.getPromoCodes()) : "");
            log.setOriginalTotal(result.getOriginalTotal());
            log.setTotalDiscount(result.getTotalDiscount());
            log.setFinalTotal(result.getFinalTotal());
            log.setManagerApprovalRequired(result.isManagerApprovalRequired());
            log.setRulesFired(result.getAppliedDiscounts().size());
            log.setAppliedDiscountsJson(objectMapper.writeValueAsString(result.getAppliedDiscounts()));
            auditLogRepository.save(log);
        } catch (Exception e) {
            this.log.error("Audit log async failed: {}", e.getMessage());
        }
    }

    // ════════════════════════════════════════
    //  SAFE DISPOSE OLD CONTAINER
    // ════════════════════════════════════════
    private void disposeOldContainerSafely(KieContainer old) {
        CompletableFuture.runAsync(() -> {
            int waited = 0;
            while (activeSessions.get() > 0 && waited < 30) {
                try {
                    Thread.sleep(1000);
                    waited++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            try {
                old.dispose();
                log.info("Old KieContainer disposed " + "after {}s", waited);
            } catch (Exception e) {
                log.warn("Old container dispose failed: {}", e.getMessage());
            }
        });
    }
}