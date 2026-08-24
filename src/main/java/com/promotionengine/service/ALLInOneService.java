package com.app.util;

import com.app.exception.ValidationException;

import java.time.*;

public final class TimezoneConverter {

    private TimezoneConverter() {}

    /**
     * Convert local date time to UTC.
     *
     * Example:
     *   input:  localDateTime = "2026-08-20T09:00:00" (9 AM)
     *   input:  timezone = "America/New_York"
     *   output: "2026-08-20T13:00:00Z" (1 PM UTC)
     *
     * DST is handled automatically by ZoneId.
     */
    public static OffsetDateTime toUtc(LocalDateTime localDateTime, String timezone) {
        validateTimezone(timezone);
        return localDateTime
            .atZone(ZoneId.of(timezone))              // attach timezone
            .withZoneSameInstant(ZoneOffset.UTC)      // convert to UTC
            .toOffsetDateTime();                      // return as OffsetDateTime
    }

    /**
     * Convert UTC back to local date time.
     *
     * Example:
     *   input:  utcDateTime = "2026-08-20T13:00:00" (1 PM UTC)
     *   input:  timezone = "America/New_York"
     *   output: "2026-08-20T09:00:00" (9 AM New York)
     */
    public static LocalDateTime toLocal(LocalDateTime utcDateTime, String timezone) {
        validateTimezone(timezone);
        return utcDateTime
            .atZone(ZoneOffset.UTC)                   // mark as UTC
            .withZoneSameInstant(ZoneId.of(timezone)) // convert to local
            .toLocalDateTime();                       // return as LocalDateTime
    }

    /**
     * Check if timezone string is valid IANA timezone.
     */
    public static void validateTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            throw new ValidationException("Timezone cannot be null or blank");
        }
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw new ValidationException("Invalid timezone: " + timezone);
        }
    }

    /**
     * Check if a date falls in a DST gap.
     * Example: 2:30 AM on March 8 doesn't exist in America/New_York
     *          (clocks jump from 2:00 AM to 3:00 AM)
     */
    public static boolean isInDstGap(LocalDateTime dateTime, String timezone) {
        ZonedDateTime zoned = dateTime.atZone(ZoneId.of(timezone));
        return !zoned.toLocalDateTime().equals(dateTime);
    }
}

===========================================================
package com.app.repository;

import com.app.dto.AmenityDTO;
import com.app.dto.LocationDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;
import java.util.Map;

@Repository
public class LocationRepository {

    private final SimpleJdbcCall getAllLocationsCall;
    private final SimpleJdbcCall getAmenitiesByLocationCall;

    public LocationRepository(JdbcTemplate jdbcTemplate) {

        this.getAllLocationsCall = new SimpleJdbcCall(jdbcTemplate)
            .withProcedureName("sp_get_all_locations")
            .returningResultSet("locations", (rs, rowNum) -> new LocationDTO(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("timezone"),
                rs.getString("city"),
                rs.getString("state"),
                rs.getString("country")
            ));

        this.getAmenitiesByLocationCall = new SimpleJdbcCall(jdbcTemplate)
            .withProcedureName("sp_get_amenities_by_location")
            .declareParameters(
                new SqlParameter("p_location_id", Types.BIGINT)
            )
            .returningResultSet("amenities", (rs, rowNum) -> new AmenityDTO(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getLong("location_function_id"),
                rs.getString("location_function_name")
            ));
    }

    // ─────────────────────────────────────────────
    // GET ALL LOCATIONS
    // ─────────────────────────────────────────────
    @Cacheable(value = "locations")
    @SuppressWarnings("unchecked")
    public List<LocationDTO> getAllLocations() {

        Map<String, Object> result = getAllLocationsCall.execute(Map.of());
        return (List<LocationDTO>) result.get("locations");
    }

    // ─────────────────────────────────────────────
    // GET AMENITIES BY LOCATION
    // ─────────────────────────────────────────────
    @Cacheable(value = "amenities", key = "#locationId")
    @SuppressWarnings("unchecked")
    public List<AmenityDTO> getAmenitiesByLocation(Long locationId) {

        Map<String, Object> result = getAmenitiesByLocationCall.execute(
            Map.of("p_location_id", locationId)
        );

        return (List<AmenityDTO>) result.get("amenities");
    }
}
===================================================
package com.app.repository;

import com.app.dto.DashboardItemDTO;
import com.app.dto.DashboardSummaryDTO;
import com.app.dto.PagedResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;
import java.util.Map;

@Repository
public class DashboardRepository {

    private final SimpleJdbcCall getDashboardForLocationCall;
    private final SimpleJdbcCall getDashboardSummaryCall;

    public DashboardRepository(JdbcTemplate jdbcTemplate) {

        this.getDashboardForLocationCall = new SimpleJdbcCall(jdbcTemplate)
            .withProcedureName("sp_get_dashboard_for_location")
            .declareParameters(
                new SqlParameter("p_location_id", Types.BIGINT),
                new SqlParameter("p_page_size", Types.INTEGER),
                new SqlParameter("p_offset", Types.INTEGER)
            )
            .returningResultSet("dashboardItems", (rs, rowNum) -> new DashboardItemDTO(
                rs.getLong("item_id"),
                rs.getString("item_type"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("discount_type"),
                rs.getBigDecimal("discount_value"),
                rs.getLong("amenity_id"),
                rs.getString("amenity_name"),
                rs.getTimestamp("start_date_utc").toLocalDateTime(),
                rs.getTimestamp("end_date_utc").toLocalDateTime(),
                rs.getBoolean("is_active"),
                rs.getString("created_by"),
                rs.getString("location_timezone"),
                rs.getTimestamp("local_start").toLocalDateTime(),
                rs.getTimestamp("local_end").toLocalDateTime(),
                rs.getTimestamp("local_now").toLocalDateTime(),
                rs.getString("status")
            ))
            .returningResultSet("totalCount", (rs, rowNum) -> rs.getLong("total_count"));

        this.getDashboardSummaryCall = new SimpleJdbcCall(jdbcTemplate)
            .withProcedureName("sp_get_dashboard_summary")
            .returningResultSet("summary", (rs, rowNum) -> new DashboardSummaryDTO(
                rs.getLong("total"),
                rs.getLong("total_promotions"),
                rs.getLong("total_system_discounts"),
                rs.getLong("active_count"),
                rs.getLong("upcoming_count"),
                rs.getLong("expired_count")
            ));
    }

    // ─────────────────────────────────────────────
    // DASHBOARD FOR LOCATION (paginated)
    // ─────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public PagedResponse<DashboardItemDTO> getDashboardForLocation(Long locationId, int pageSize, int offset) {

        Map<String, Object> result = getDashboardForLocationCall.execute(Map.of(
            "p_location_id", locationId,
            "p_page_size", pageSize,
            "p_offset", offset
        ));

        List<DashboardItemDTO> items = (List<DashboardItemDTO>) result.get("dashboardItems");
        List<Long> counts = (List<Long>) result.get("totalCount");

        long totalCount = (counts != null && !counts.isEmpty()) ? counts.get(0) : 0L;

        return PagedResponse.of(items, totalCount, offset / pageSize, pageSize);
    }

    // ─────────────────────────────────────────────
    // DASHBOARD SUMMARY
    // ─────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public DashboardSummaryDTO getDashboardSummary() {

        Map<String, Object> result = getDashboardSummaryCall.execute(Map.of());

        List<DashboardSummaryDTO> list = (List<DashboardSummaryDTO>) result.get("summary");

        if (list == null || list.isEmpty()) {
            return new DashboardSummaryDTO(0L, 0L, 0L, 0L, 0L, 0L);
        }

        return list.get(0);
    }
}
================================================================
package com.app.repository;

import com.app.dto.ActivePromotionDTO;
import com.app.dto.PromotionDetailDTO;
import com.app.exception.PromotionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class SystemDiscountRepository {

    private static final Logger log = LoggerFactory.getLogger(SystemDiscountRepository.class);

    private final SimpleJdbcCall createSystemDiscountCall;
    private final SimpleJdbcCall updateSystemDiscountDatesCall;
    private final SimpleJdbcCall getSystemDiscountDetailCall;
    private final SimpleJdbcCall getActiveSystemDiscountsCall;
    private final SimpleJdbcCall getLocationIdCall;

    public SystemDiscountRepository(JdbcTemplate jdbcTemplate) {

        this.createSystemDiscountCall = new SimpleJdbcCall(jdbcTemplate)
            .withProcedureName("sp_create_system_discount")
            .declareParameters(
                new SqlParameter("p_location_id", Types.BIGINT),
                new SqlParameter("p_amenity_id", Types.BIGINT),
                new SqlParameter("p_name", Types.VARCHAR),
                new SqlParameter("p_description", Types.VARCHAR),
                new SqlParameter("p_discount_type", Types.VARCHAR),
                new SqlParameter("p_discount_value", Types.DECIMAL),
                new SqlParameter("p_start_date_utc", Types.TIMESTAMP),
                new SqlParameter("p_end_date_utc", Types.TIMESTAMP),
                new SqlParameter("p_created_by", Types.VARCHAR),
                new SqlOutParameter("p_discount_id", Types.BIGINT)
            );

        this.updateSystemDiscountDatesCall = new SimpleJdbcCall(jdbcTemplate)
            .withProcedureName("sp_update_system_discount_dates")
            .declareParameters(
                new SqlParameter("p_id", Types.BIGINT),
                new SqlParameter("p_start_date_utc", Types.TIMESTAMP),
                new SqlParameter("p_end_date_utc", Types.TIMESTAMP)
            );

        this.getSystemDiscountDetailCall = new SimpleJdbcCall(jdbcTemplate)
            .withProcedureName("sp_get_system_discount_detail")
            .declareParameters(
                new SqlParameter("p_id", Types.BIGINT)
            )
            .returningResultSet("discountDetail", (rs, rowNum) -> new PromotionDetailDTO(
                rs.getLong("id"),
                rs.getLong("location_id"),
                rs.getString("location_name"),
                rs.getLong("amenity_id"),
                rs.getString("amenity_name"),
                rs.getString("location_function_name"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("discount_type"),
                rs.getBigDecimal("discount_value"),
                rs.getTimestamp("start_date_utc").toLocalDateTime(),
                rs.getTimestamp("end_date_utc").toLocalDateTime(),
                rs.getBoolean("is_active"),
                rs.getString("created_by"),
                rs.getString("location_timezone"),
                rs.getTimestamp("local_start").toLocalDateTime(),
                rs.getTimestamp("local_end").toLocalDateTime(),
                rs.getTimestamp("local_now").toLocalDateTime(),
                rs.getString("status")
            ));

        this.getActiveSystemDiscountsCall = new SimpleJdbcCall(jdbcTemplate)
            .withProcedureName("sp_get_active_system_discounts_for_location")
            .declareParameters(
                new SqlParameter("p_location_id", Types.BIGINT)
            )
            .returningResultSet("activeDiscounts", (rs, rowNum) -> new ActivePromotionDTO(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("discount_type"),
                rs.getBigDecimal("discount_value"),
                rs.getLong("amenity_id"),
                rs.getString("amenity_name"),
                rs.getString("location_function_name"),
                rs.getTimestamp("start_date_utc").toLocalDateTime(),
                rs.getTimestamp("end_date_utc").toLocalDateTime(),
                rs.getString("location_timezone"),
                rs.getTimestamp("local_start").toLocalDateTime(),
                rs.getTimestamp("local_end").toLocalDateTime(),
                rs.getTimestamp("local_now").toLocalDateTime()
            ));

        this.getLocationIdCall = new SimpleJdbcCall(jdbcTemplate)
            .withProcedureName("sp_get_location_id_for_system_discount")
            .declareParameters(
                new SqlParameter("p_discount_id", Types.BIGINT),
                new SqlOutParameter("p_location_id", Types.BIGINT)
            );
    }

    // ─────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 200))
    public Long createSystemDiscount(Long locationId, Long amenityId,
                                     String name, String description,
                                     String discountType, BigDecimal discountValue,
                                     OffsetDateTime startDateUtc, OffsetDateTime endDateUtc,
                                     String createdBy) {

        Map<String, Object> result = createSystemDiscountCall.execute(Map.of(
            "p_location_id", locationId,
            "p_amenity_id", amenityId,
            "p_name", name,
            "p_description", description != null ? description : "",
            "p_discount_type", discountType,
            "p_discount_value", discountValue,
            "p_start_date_utc", Timestamp.valueOf(startDateUtc.toLocalDateTime()),
            "p_end_date_utc", Timestamp.valueOf(endDateUtc.toLocalDateTime()),
            "p_created_by", createdBy != null ? createdBy : "SYSTEM"
        ));

        Long id = (Long) result.get("p_discount_id");
        log.info("Created system discount id={} location={} amenity={}", id, locationId, amenityId);
        return id;
    }

    // ─────────────────────────────────────────────
    // UPDATE DATES
    // ─────────────────────────────────────────────
    public void updateSystemDiscountDates(Long discountId,
                                          OffsetDateTime startDateUtc,
                                          OffsetDateTime endDateUtc) {

        updateSystemDiscountDatesCall.execute(Map.of(
            "p_id", discountId,
            "p_start_date_utc", Timestamp.valueOf(startDateUtc.toLocalDateTime()),
            "p_end_date_utc", Timestamp.valueOf(endDateUtc.toLocalDateTime())
        ));

        log.info("Updated dates for system discount {}", discountId);
    }

    // ─────────────────────────────────────────────
    // GET LOCATION ID
    // ─────────────────────────────────────────────
    public Long getLocationIdForDiscount(Long discountId) {

        Map<String, Object> result = getLocationIdCall.execute(
            Map.of("p_discount_id", discountId)
        );

        Long locationId = (Long) result.get("p_location_id");

        if (locationId == null) {
            throw new PromotionNotFoundException("System discount not found: " + discountId);
        }

        return locationId;
    }

    // ─────────────────────────────────────────────
    // GET DETAIL
    // ─────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public PromotionDetailDTO getSystemDiscountDetail(Long discountId) {

        Map<String, Object> result = getSystemDiscountDetailCall.execute(
            Map.of("p_id", discountId)
        );

        List<PromotionDetailDTO> list = (List<PromotionDetailDTO>) result.get("discountDetail");

        if (list == null || list.isEmpty()) {
            throw new PromotionNotFoundException("System discount not found: " + discountId);
        }

        return list.get(0);
    }

    // ─────────────────────────────────────────────
    // GET ACTIVE FOR LOCATION
    // ─────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public List<ActivePromotionDTO> getActiveForLocation(Long locationId) {

        Map<String, Object> result = getActiveSystemDiscountsCall.execute(
            Map.of("p_location_id", locationId)
        );

        return (List<ActivePromotionDTO>) result.get("activeDiscounts");
    }
}
=================================================

package com.app.repository;

import com.app.exception.TimezoneNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.Map;

@Repository
public class TimezoneRepository {

    private static final Logger log = LoggerFactory.getLogger(TimezoneRepository.class);

    private final SimpleJdbcCall getTimezoneByLocationCall;

    public TimezoneRepository(JdbcTemplate jdbcTemplate) {
        this.getTimezoneByLocationCall = new SimpleJdbcCall(jdbcTemplate)
            .withProcedureName("sp_get_timezone_by_location")
            .declareParameters(
                new SqlParameter("p_location_id", Types.BIGINT),
                new SqlOutParameter("p_timezone", Types.VARCHAR)
            );
    }

    @Cacheable(value = "timezones", key = "#locationId")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 100))
    public String getTimezoneByLocation(Long locationId) {

        Map<String, Object> result = getTimezoneByLocationCall.execute(
            Map.of("p_location_id", locationId)
        );

        String timezone = (String) result.get("p_timezone");

        if (timezone == null) {
            throw new TimezoneNotFoundException("No timezone found for location: " + locationId);
        }

        log.debug("Timezone for location {}: {}", locationId, timezone);
        return timezone;
    }
}
===========================================
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

    // ─────────────────────────────────────────────
    // CREATE SYSTEM DISCOUNT
    // ─────────────────────────────────────────────
    @Transactional
    public Long createSystemDiscount(CreateSystemDiscountRequest request) {

        // Validate
        if (!request.startDate().isBefore(request.endDate())) {
            throw new ValidationException("Start date must be before end date");
        }

        // Get timezone for this location
        String timezone = timezoneRepository.getTimezoneByLocation(request.locationId());

        // Convert local → UTC
        OffsetDateTime startUtc = TimezoneConverter.toUtc(request.startDate(), timezone);
        OffsetDateTime endUtc = TimezoneConverter.toUtc(request.endDate(), timezone);

        // Create
        Long id = repository.createSystemDiscount(
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

        log.info("Created system discount id={} location={} amenity={}",
            id, request.locationId(), request.amenityId());

        return id;
    }

    // ─────────────────────────────────────────────
    // UPDATE DATES
    // ─────────────────────────────────────────────
    @Transactional
    public void updateDates(Long discountId, UpdateDatesRequest request) {

        if (!request.newStartDate().isBefore(request.newEndDate())) {
            throw new ValidationException("Start date must be before end date");
        }

        // Get location for this discount
        Long locationId = repository.getLocationIdForDiscount(discountId);

        // Get timezone
        String timezone = timezoneRepository.getTimezoneByLocation(locationId);

        // Convert local → UTC
        OffsetDateTime startUtc = TimezoneConverter.toUtc(request.newStartDate(), timezone);
        OffsetDateTime endUtc = TimezoneConverter.toUtc(request.newEndDate(), timezone);

        // Update
        repository.updateSystemDiscountDates(discountId, startUtc, endUtc);

        log.info("Updated dates for system discount {}", discountId);
    }

    // ─────────────────────────────────────────────
    // GET DETAIL
    // ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PromotionDetailDTO getDetail(Long discountId) {
        return repository.getSystemDiscountDetail(discountId);
    }

    // ─────────────────────────────────────────────
    // GET ACTIVE FOR LOCATION
    // ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ActivePromotionDTO> getActiveForLocation(Long locationId) {
        return repository.getActiveForLocation(locationId);
    }
}
======================================
╔══════════════════════════════════════════════════════════════════╗
║                                                                  ║
║   SERVICE                  WHAT IT DOES                          ║
║   ────────────────────────────────────────────────────────       ║
║                                                                  ║
║   PromotionService         • createPromotion()                   ║
║                            • updateDates()                       ║
║                            • getDetail()                         ║
║                            • getActiveForLocation()              ║
║                                                                  ║
║   SystemDiscountService    • createSystemDiscount()              ║
║                            • updateDates()                       ║
║                            • getDetail()                         ║
║                            • getActiveForLocation()              ║
║                                                                  ║
║   DashboardService         • getDashboardForLocation()           ║
║                            • getSummary()                        ║
║                                                                  ║
║   ALL services use:                                              ║
║   • TimezoneRepository (cached timezone lookup)                  ║
║   • TimezoneConverter.toUtc() (local → UTC conversion)           ║
║                                                                  ║
╚══════════════════════════════════════════════════════════════════╝

==========================================
package com.app.service;

import com.app.dto.*;
import com.app.repository.DashboardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final DashboardRepository repository;

    public DashboardService(DashboardRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<DashboardItemDTO> getDashboardForLocation(Long locationId, int page, int size) {
        int offset = page * size;
        return repository.getDashboardForLocation(locationId, size, offset);
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDTO getSummary() {
        return repository.getDashboardSummary();
    }
}

===========================================

package com.app.dto;

import com.app.entity.DiscountType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateSystemDiscountRequest(

    @NotNull
    Long locationId,

    @NotNull
    Long amenityId,

    @NotBlank @Size(max = 255)
    String name,

    @Size(max = 5000)
    String description,

    @NotNull
    DiscountType discountType,

    @NotNull @DecimalMin("0.01")
    BigDecimal discountValue,

    @NotNull
    LocalDateTime startDate,

    @NotNull
    LocalDateTime endDate,

    @Size(max = 100)
    String createdBy
) {}

==================================
package com.app.dto;

import java.util.List;

public record PagedResponse<T>(
    List<T> data,
    long totalElements,
    int page,
    int size,
    int totalPages
) {
    public static <T> PagedResponse<T> of(List<T> data, long totalElements, int page, int size) {
        return new PagedResponse<>(data, totalElements, page, size,
            (int) Math.ceil((double) totalElements / size));
    }
}
===============================================================================================================================================================
package com.app.dto;

public record DashboardSummaryDTO(
    Long total,
    Long totalPromotions,
    Long totalSystemDiscounts,
    Long activeCount,
    Long upcomingCount,
    Long expiredCount
) {}
===========================
package com.app.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DashboardItemDTO(
    Long itemId,
    String itemType,
    String name,
    String description,
    String discountType,
    BigDecimal discountValue,
    Long amenityId,
    String amenityName,
    LocalDateTime startDateUtc,
    LocalDateTime endDateUtc,
    Boolean isActive,
    String createdBy,
    String locationTimezone,
    LocalDateTime localStart,
    LocalDateTime localEnd,
    LocalDateTime localNow,
    String status
) {}
===============================
package com.app.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionDetailDTO(
    Long id,
    Long locationId,
    String locationName,
    Long amenityId,
    String amenityName,
    String locationFunctionName,
    String name,
    String description,
    String discountType,
    BigDecimal discountValue,
    LocalDateTime startDateUtc,
    LocalDateTime endDateUtc,
    Boolean isActive,
    String createdBy,
    String locationTimezone,
    LocalDateTime localStart,
    LocalDateTime localEnd,
    LocalDateTime localNow,
    String status
) {}
--------------------------------------
