package hlt.promotion.engine.client;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import hlt.promotion.engine.exception.HltApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionApiClient {

    private final RestClient hltRestClient;

    public Map<String, Object> getPromotionByCode(
            String sessionKey, String promoCode) {

        return execute(() -> hltRestClient.get()
                .uri("/api/v1/promotions/code/{promoCode}", promoCode)
                .header("X-Session-Key", sessionKey)
                .retrieve()
                .body(new ParameterizedTypeReference<
                        Map<String, Object>>() {}));
    }

    public String getTimezone(
            String sessionKey, Integer locationId) {

        return execute(() -> hltRestClient.get()
                .uri("/api/v1/locations/{locationId}/timezone",
                        locationId)
                .header("X-Session-Key", sessionKey)
                .retrieve()
                .body(String.class));
    }

    public Map<String, Object> createPromotion(
            String sessionKey,
            CreatePromotionRequest request) {

        return execute(() -> hltRestClient.post()
                .uri("/api/v1/promotions")
                .header("X-Session-Key", sessionKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<
                        Map<String, Object>>() {}));
    }

    private <T> T execute(
            java.util.function.Supplier<T> operation) {

        try {
            return operation.get();
        } catch (RestClientResponseException ex) {
            log.error("hlt-api returned HTTP status {}",
                    ex.getStatusCode().value());

            throw new HltApiException(
                    "hlt-api request failed with HTTP status "
                            + ex.getStatusCode().value(),
                    ex);
        } catch (ResourceAccessException ex) {
            log.error("Unable to connect to hlt-api");

            throw new HltApiException(
                    "Unable to connect to hlt-api", ex);
        }
    }
}
