package com.paypilot.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Transaction(
        String id,
        String customer,
        BigDecimal amount,
        String currency,
        String type,
        String status,
        LocalDateTime createdAt) {
}
