package com.paypilot.controller;

import com.paypilot.service.IntentRouterService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class IntentRouterController {

    private final IntentRouterService intentRouterService;

    public IntentRouterController(
            IntentRouterService intentRouterService) {
        this.intentRouterService = intentRouterService;
    }

    @PostMapping("/intent")
    public Map<String, String> classify(
            @RequestBody Map<String, String> request) {

        String message = request.getOrDefault("message", "");

        IntentRouterService.Intent intent = intentRouterService.classify(message);

        return Map.of("intent", intent.name());
    }
}
