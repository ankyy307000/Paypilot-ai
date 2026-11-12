
package com.paypilot.controller;

import com.paypilot.service.PayPalAuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/paypal")
public class PayPalController {

    private final PayPalAuthService authService;

    public PayPalController(PayPalAuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        boolean connected = authService.verifyConnection();

        return Map.of(
                "provider", "PayPal",
                "environment", "SANDBOX",
                "connected", connected);
    }
}
