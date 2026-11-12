package com.paypilot.service;

import tools.jackson.databind.ObjectMapper;
import com.paypilot.model.ActionPlan;
import org.springframework.stereotype.Service;

@Service
public class ActionPlannerService {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    public ActionPlannerService(
            GeminiService geminiService,
            ObjectMapper objectMapper) {
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
    }

    public ActionPlan plan(String message) {

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Message cannot be empty");
        }

        String prompt = """
                You are PayPilot's financial action planner.

                Extract the user's requested financial action.

                Return ONLY a valid JSON object with these fields:
                {
                  "action": "REFUND or UNKNOWN",
                  "captureId": "string or null",
                  "amount": "decimal string or null",
                  "currency": "USD",
                  "requiresApproval": true
                }

                Rules:
                - Do not invent missing values.
                - Use REFUND only when explicitly requested.
                - Return UNKNOWN for unsupported actions.
                - Never execute any financial action.
                - All refunds require human approval.
                - Do not include markdown or explanations.

                User request:
                %s
                """.formatted(message);

        try {
            String response = geminiService.ask(prompt);

            // Handle optional markdown code fences.
            String json = response.trim()
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            return objectMapper.readValue(json, ActionPlan.class);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to generate action plan",
                    ex);
        }
    }
}
