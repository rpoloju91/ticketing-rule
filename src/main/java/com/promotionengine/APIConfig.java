package hlt.promo.engine.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;

@Configuration
@Slf4j
public class HltApiConfig {

Temporary exception for DEV environment to support ECS service-to-service communication with hlt-api while the permanent SSL certificate/trust configuration is being finalized. This configuration is not intended for production and will be removed once the ECS service-to-service SSL solution is implemented.    
    @Bean
    public RestClient hltRestClient(
            @Value("${hlt-api.base-url}") String baseUrl,
            @Value("${hlt-api.connect-timeout:5s}")
            Duration connectionTimeout,
            @Value("${hlt-api.read-timeout:15s}")
            Duration readTimeout,
            @Value("${hlt-api.skip-ssl:false}")
            boolean skipSsl) throws Exception {

        log.info("Creating hlt-api RestClient");
        log.info("hlt-api base URL: {}", baseUrl);
        log.info("hlt-api skip SSL: {}", skipSsl);

        HttpClient.Builder httpClientBuilder =
                HttpClient.newBuilder()
                        .connectTimeout(connectionTimeout);

        if (skipSsl) {

            log.warn(
                    "TEMPORARY: SSL certificate and hostname validation " +
                    "is disabled for hlt-api");

            SSLContext sslContext =
                    createTrustAllSslContext();

            SSLParameters sslParameters =
                    new SSLParameters();

            // TEMPORARY:
            // Disable HTTPS hostname verification.
            sslParameters.setEndpointIdentificationAlgorithm(null);

            httpClientBuilder
                    .sslContext(sslContext)
                    .sslParameters(sslParameters);
        }

        HttpClient httpClient =
                httpClientBuilder.build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }


    /**
     * TEMPORARY DEVELOPMENT ONLY.
     *
     * Trusts all certificates.
     * This must be removed once the proper ECS
     * service-to-service SSL configuration is available.
     */
    private SSLContext createTrustAllSslContext()
            throws Exception {

        TrustManager trustManager =
                new X509TrustManager() {

                    @Override
                    public void checkClientTrusted(
                            X509Certificate[] chain,
                            String authType) {
                        // TEMPORARY
                    }

                    @Override
                    public void checkServerTrusted(
                            X509Certificate[] chain,
                            String authType) {
                        // TEMPORARY
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                };

        SSLContext sslContext =
                SSLContext.getInstance("TLS");

        sslContext.init(
                null,
                new TrustManager[]{trustManager},
                new SecureRandom());

        return sslContext;
    }
}
