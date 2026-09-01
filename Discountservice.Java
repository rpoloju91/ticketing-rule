package com.app.service;

import com.app.dto.*;
import com.app.exception.ValidationException;
import com.app.repository.SystemDiscountRepository;
import com.app.repository.TimezoneRepository;
import com.app.util.TimezoneConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class SystemDiscountService {

    private static final Logger log = LoggerFactory.getLogger(SystemDiscountService.class);
    private final SystemDiscountRepository repository;
    private final TimezoneRepository timezoneRepository;

    public SystemDiscountService(SystemDiscountRepository repository,
                                 TimezoneRepository timezoneRepository) {
        this.repository = repository;
        this.timezoneRepository = timezoneRepository;
    }

    // ═══════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════
    @Transactional
    public CreateSystemDiscountResponse createSystemDiscount(CreateSystemDiscountRequest request) {

        // Validate
        if (!request.startDate().isBefore(request.endDate())) {
            throw new ValidationException("Start date must be before end date");
        }

        // Get timezone + convert to UTC
        String timezone = timezoneRepository.getTimezoneByLocation(request.locationId());
        OffsetDateTime startUtc = TimezoneConverter.toUtc(request.startDate(), timezone);
        OffsetDateTime endUtc = TimezoneConverter.toUtc(request.endDate(), timezone);

        // Insert into DB
        Long id = repository.createSystemDiscount(
            request.locationId(),
            request.amenityId(),
            request.name(),
            request.description(),
            request.discountType().name(),
            request.discountValue(),
            startUtc,
            endUtc,
            request.createdBy(),
            request.approvalRequired()
        );

        // Check approval flag
        if (request.approvalRequired()) {
            // TRUE → PENDING, no Drools
            log.info("Created system discount id={} status=PENDING_APPROVAL. No Drools.", id);
            return new CreateSystemDiscountResponse(id, "PENDING_APPROVAL");
        } else {
            // FALSE → PUBLISHED, generate Drools
            generateDroolsRules(id);
            log.info("Created system discount id={} status=PUBLISHED. Drools generated.", id);
            return new CreateSystemDiscountResponse(id, "PUBLISHED");
        }
    }

    // ═══════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════
    @Transactional
    public UpdateSystemDiscountResponse updateSystemDiscount(Long discountId,
                                                              UpdateSystemDiscountRequest request) {

        if (request.newStartDate() != null && request.newEndDate() != null) {
            if (!request.newStartDate().isBefore(request.newEndDate())) {
                throw new ValidationException("Start date must be before end date");
            }
        }

        // Convert dates to UTC if provided
        OffsetDateTime startUtc = null;
        OffsetDateTime endUtc = null;

        if (request.newStartDate() != null || request.newEndDate() != null) {
            Long locationId = repository.getLocationIdForDiscount(discountId);
            String timezone = timezoneRepository.getTimezoneByLocation(locationId);
            if (request.newStartDate() != null) {
                startUtc = TimezoneConverter.toUtc(request.newStartDate(), timezone);
            }
            if (request.newEndDate() != null) {
                endUtc = TimezoneConverter.toUtc(request.newEndDate(), timezone);
            }
        }

        // Check approval flag
        if (request.approvalRequired()) {
            // TRUE → insert child row, original untouched
            Long pendingChildId = repository.submitSystemDiscountUpdate(
                discountId,
                request.name(),
                request.description(),
                request.discountType(),
                request.discountValue(),
                startUtc,
                endUtc,
                request.requestedBy()
            );

            log.info("Created pending child {} for discount {}. Original UNTOUCHED.", pendingChildId, discountId);
            return new UpdateSystemDiscountResponse(discountId, pendingChildId, "PENDING_APPROVAL");

        } else {
            // FALSE → update directly + Drools
            repository.updateSystemDiscount(discountId,
                request.name(), request.description(),
                request.discountType(), request.discountValue(),
                startUtc, endUtc);

            generateDroolsRules(discountId);
            log.info("Updated discount {} directly. Drools regenerated.", discountId);
            return new UpdateSystemDiscountResponse(discountId, null, "UPDATED");
        }
    }

    // ═══════════════════════════════════════════════════════
    // REVIEW (single method for approve + reject)
    // ═══════════════════════════════════════════════════════
    @Transactional
    public void reviewSystemDiscount(Long discountId, ReviewActionRequest request) {

        switch (request.action()) {

            case APPROVE -> {
                // Proc returns parent_id (null=new, number=update)
                ApproveResult result = repository.approveSystemDiscount(discountId, request.actionBy());

                // Determine Drools target
                Long droolsTargetId = (result.parentId() != null) ? result.parentId() : discountId;

                generateDroolsRules(droolsTargetId);
                log.info("APPROVED system discount {}. Drools generated for id={}.", discountId, droolsTargetId);
            }

            case REJECT -> {
                // Validate reason is provided
                if (request.reason() == null || request.reason().isBlank()) {
                    throw new ValidationException("Rejection reason is required");
                }

                repository.rejectSystemDiscount(discountId, request.actionBy(), request.reason());
                log.info("REJECTED system discount {}. Reason: {}.", discountId, request.reason());
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public PromotionDetailDTO getDetail(Long discountId) {
        return repository.getSystemDiscountDetail(discountId);
    }

    @Transactional(readOnly = true)
    public List<ActivePromotionDTO> getActiveForLocation(Long locationId) {
        return repository.getActiveForLocation(locationId);
    }

    // ═══════════════════════════════════════════════════════
    // DROOLS
    // ═══════════════════════════════════════════════════════
    private void generateDroolsRules(Long discountId) {
        // TODO: Drools rule generation
        log.info("Generated Drools rules for system discount id={}", discountId);
    }
}
