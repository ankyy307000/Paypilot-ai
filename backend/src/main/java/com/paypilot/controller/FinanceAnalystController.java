package com.paypilot.controller;

import com.paypilot.service.FinanceAnalystService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class FinanceAnalystController {

    private final FinanceAnalystService analystService;

    public FinanceAnalystController(
            FinanceAnalystService analystService) {
        this.analystService = analystService;
    }

    @PostMapping("/analyze")
    public Map<String, String> analyze(
            @RequestBody Map<String, String> request) {

        String question = request.get("message");

        if (question == null || question.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message cannot be empty");
        }

        return Map.of(
                "response", analystService.analyze(question));
    }
}
