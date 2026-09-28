
//The hlt-api must expose the endpoints called by PromotionApiClient.@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionApiService promotionApiService;

    @GetMapping("/code/{promoCode}")
    public ResponseEntity<Map<String, Object>> getByCode(
            @RequestHeader("X-Session-Key") String sessionKey,
            @PathVariable String promoCode) {

        return ResponseEntity.ok(
                promotionApiService.getByCode(
                        sessionKey, promoCode));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestHeader("X-Session-Key") String sessionKey,
            @RequestBody CreatePromotionRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(promotionApiService.create(
                        sessionKey, request));
    }
}
