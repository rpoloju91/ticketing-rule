@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionServiceNew {

    private final PromotionApiClient promotionApiClient;
    private final DroolsRuleService droolsRuleService;
    private final ObjectMapper objectMapper;

    public void createPromotion(CreatePromotionRequest request) {

        // 1. Validate dates
        if (!request.getStartDate().isBefore(request.getEndDate())) {
            throw new ValidationException(
                    "Start date must be before end date");
        }

        // 2. Check duplicate promo code through hlt-api
        Map<String, Object> existing =
                promotionApiClient.getPromotionByCode(
                        request.getSessionKey(),
                        request.getPromoCode());

        if (existing != null && !existing.isEmpty()) {
            throw new ValidationException(
                    "Promo code " + request.getPromoCode()
                            + " already exists");
        }

        // 3. Get timezone through hlt-api
        String timezone = promotionApiClient.getTimezone(
                request.getSessionKey(),
                request.getLocationId().intValue());

        // 4. Convert local dates to UTC
        request.setStartDate(
                TimezoneConverter.toUtc(
                        request.getStartDate(), timezone)
                        .toLocalDateTime());

        request.setEndDate(
                TimezoneConverter.toUtc(
                        request.getEndDate(), timezone)
                        .toLocalDateTime());

        // 5. Create promotion through hlt-api
        Map<String, Object> promotion =
                promotionApiClient.createPromotion(
                        request.getSessionKey(), request);

        Promotion promotionObj =
                objectMapper.convertValue(
                        promotion, Promotion.class);

        log.info(
                "Promotion created location={} amenity={} promoCode={}",
                request.getLocationId(),
                request.getAmenityId(),
                request.getPromoCode());

        // 6. Generate Drools rule if published
        if (PromotionStatus.PUBLISHED.name()
                .equals(request.getPromoStatus())) {

            droolsRuleService.generatePromotionRule(
                    promotionObj, request.getChangedBy());
        }
    }
}
