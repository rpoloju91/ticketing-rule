package com.app.service;

import com.app.dto.*;
import com.app.exception.ValidationException;
import com.app.repository.PromotionRepository;
import com.app.repository.TimezoneRepository;
import com.app.util.TimezoneConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class PromotionService {

    private static final Logger log = LoggerFactory.getLogger(PromotionService.class);
    private final PromotionRepository repository;
    private final TimezoneRepository timezoneRepository;

    public PromotionService(PromotionRepository repository,
                            TimezoneRepository timezoneRepository) {
        this.repository = repository;
        this.timezoneRepository = timezoneRepository;
    }

    @Transactional
    public Long createPromotion(CreatePromotionRequest request) {

        // Validate
        if (!request.startDate().isBefore(request.endDate())) {
            throw new ValidationException("Start date must be before end date");
        }

        // Get timezone for this ONE location
        String timezone = timezoneRepository
            .getTimezoneByLocation(request.locationId());

        // Convert local → UTC (DST handled automatically)
        OffsetDateTime startUtc = TimezoneConverter
            .toUtc(request.startDate(), timezone);
        OffsetDateTime endUtc = TimezoneConverter
            .toUtc(request.endDate(), timezone);

        // Create ONE row
        Long id = repository.createPromotion(
            request.locationId(),
            request.amenityId(),
            request.name(),
            request.description(),
            request.discountType().name(),
            request.discountValue(),
            startUtc,
            endUtc,
            request.createdBy()
        );

        log.info("Created promotion id={} location={} amenity={}",
            id, request.locationId(), request.amenityId());

        return id;
    }

    @Transactional
    public void updatePromotionDates(Long promotionId, UpdateDatesRequest request) {

        if (!request.newStartDate().isBefore(request.newEndDate())) {
            throw new ValidationException("Start date must be before end date");
        }

        // Get location_id for this promotion
        Long locationId = repository.getLocationIdForPromotion(promotionId);

        // Get timezone
        String timezone = timezoneRepository.getTimezoneByLocation(locationId);

        // Convert
        OffsetDateTime startUtc = TimezoneConverter
            .toUtc(request.newStartDate(), timezone);
        OffsetDateTime endUtc = TimezoneConverter
            .toUtc(request.newEndDate(), timezone);

        // Update
        repository.updatePromotionDates(promotionId, startUtc, endUtc);

        log.info("Updated dates for promotion {}", promotionId);
    }
}
