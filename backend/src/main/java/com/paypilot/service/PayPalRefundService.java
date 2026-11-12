
package com.paypilot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class PayPalRefundService {

    private final RestClient client;
    private final String basicAuth;

    public PayPalRefundService(
            @Value("${paypal.base-url}") String baseUrl,
            @Value("${paypal.client-id}") String clientId,
            @Value("${paypal.client-secret}") String clientSecret) {

        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        String credentials = clientId + ":" + clientSecret;
        this.basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private String accessToken() {
        Map<?, ?> result = client.post()
                .uri("/v1/oauth2/token")
                .header("Authorization", basicAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials")
                .retrieve()
                .body(Map.class);

        if (result == null || !(result.get("access_token") instanceof String token)) {
            throw new IllegalStateException("PayPal OAuth failed");
        }

        return token;
    }

    public Map<?, ?> getCapture(String captureId) {
        return client.get()
                .uri("/v2/payments/captures/{id}", captureId)
                .headers(h -> h.setBearerAuth(accessToken()))
                .retrieve()
                .body(Map.class);
    }

    public Map<?, ?> refund(
            String captureId,
            BigDecimal amount,
            String currency,
            String requestId) {

        Map<String, Object> payload = Map.of(
                "amount", Map.of(
                        "value", amount.toPlainString(),
                        "currency_code", currency));

        return client.post()
                .uri("/v2/payments/captures/{id}/refund", captureId)
                .headers(h -> h.setBearerAuth(accessToken()))
                .header("PayPal-Request-Id", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);
    }
}
