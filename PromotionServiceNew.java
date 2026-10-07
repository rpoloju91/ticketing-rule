@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionServiceNew {

    private final PromotionApiClient promotionApiClient;
    private final DroolsRuleService droolsRuleService;
    private final ObjectMapper objectMapper;


    public ApiResponse<?> createPromotion(
            CreatePromotionRequest request) {

        validateRequest(request);


        // Check duplicate promo code
        Map<String, Object> existing =
                promotionApiClient.getPromotionByCode(
                        request.getSessionKey(),
                        request.getPromoCode());

        if (existing != null && !existing.isEmpty()) {

            throw new BusinessException(
                    "Promo code "
                            + request.getPromoCode()
                            + " already exists");
        }


        // Get timezone from hlt-api
        String timezone =
                promotionApiClient.getTimezone(
                        request.getSessionKey(),
                        request.getLocationId()
                                .intValue());


        // Convert local date to UTC
        request.setStartDate(
                TimezoneConverter.toUtc(
                        request.getStartDate(),
                        timezone)
                        .toLocalDateTime());

        request.setEndDate(
                TimezoneConverter.toUtc(
                        request.getEndDate(),
                        timezone)
                        .toLocalDateTime());


        // Create promotion through hlt-api
        Map<String, Object> promotion =
                promotionApiClient.createPromotion(
                        request.getSessionKey(),
                        request);


        Promotion promotionObj =
                objectMapper.convertValue(
                        promotion,
                        Promotion.class);


        // Generate Drools rule
        if (PromotionStatus.PUBLISHED.name()
                .equals(request.getPromoStatus())) {

            droolsRuleService.generatePromotionRule(
                    promotionObj,
                    request.getChangedBy());
        }


        log.info(
                "Promotion created successfully. location={}, amenity={}, promoCode={}",
                request.getLocationId(),
                request.getAmenityId(),
                request.getPromoCode());


        return ApiResponse.success(
                "Promotion Created",
                promotionObj);
    }


    private void validateRequest(
            CreatePromotionRequest request) {

        if (request == null) {

            throw new BusinessException(
                    "Promotion request cannot be null");
        }


        if (request.getStartDate() == null
                || request.getEndDate() == null) {

            throw new BusinessException(
                    "Start date and end date are required");
        }


        if (!request.getStartDate()
                .isBefore(request.getEndDate())) {

            throw new BusinessException(
                    "Start date must be before end date");
        }


        if (request.getPromoCode() == null
                || request.getPromoCode().isBlank()) {

            throw new BusinessException(
                    "Promo code is required");
        }
    }
}
