
package com.paypilot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class PayPalAuthService {

    private final RestClient client;

    public PayPalAuthService(
            @Value("${paypal.base-url}") String baseUrl,
            @Value("${paypal.client-id}") String clientId,
            @Value("${paypal.client-secret}") String clientSecret) {

        String credentials = clientId + ":" + clientSecret;

        String basicAuth = Base64.getEncoder()
                .encodeToString(
                        credentials.getBytes(StandardCharsets.UTF_8));

        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Basic " + basicAuth)
                .build();
    }

    public Map<?, ?> getAccessTokenResponse() {
        return client.post()
                .uri("/v1/oauth2/token")
                .contentType(
                        MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials")
                .retrieve()
                .body(Map.class);
    }

    public boolean verifyConnection() {
        Map<?, ?> response = getAccessTokenResponse();

        return response != null
                && response.get("access_token") instanceof String;
    }
}
