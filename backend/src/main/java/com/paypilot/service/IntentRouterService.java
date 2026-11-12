package com.paypilot.service;

import org.springframework.stereotype.Service;
import java.util.Locale;

@Service
public class IntentRouterService {

    public enum Intent {
        ANALYZE,
        REFUND,
        UNKNOWN
    }

    private final GeminiService geminiService;

    public IntentRouterService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    public Intent classify(String message) {

        if (message == null || message.isBlank()) {
            return Intent.UNKNOWN;
        }

        String prompt = """
                You are the intent router for PayPilot AI,
                a financial management assistant.

                Classify the user's request into exactly one:

                ANALYZE: Questions about transactions,
                spending, revenue, trends or financial insights.

                REFUND: Requests to refund or reverse a payment.

                UNKNOWN: Anything else, including transfers,
                payouts, unrelated questions or unclear requests.

                Return ONLY one word:
                ANALYZE, REFUND or UNKNOWN.

                User request:
                """ + message;

        String response = geminiService.ask(prompt);

        if (response == null) {
            return Intent.UNKNOWN;
        }

        String result = response.trim()
                .replace("`", "")
                .toUpperCase(Locale.ROOT);

        return switch (result) {
            case "ANALYZE" -> Intent.ANALYZE;
            case "REFUND" -> Intent.REFUND;
            default -> Intent.UNKNOWN;
        };
    }
}
