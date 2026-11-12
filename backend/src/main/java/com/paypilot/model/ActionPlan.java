package com.paypilot.model;

public record ActionPlan(
        String action,
        String captureId,
        String amount,
        String currency,
        boolean requiresApproval) {
}
