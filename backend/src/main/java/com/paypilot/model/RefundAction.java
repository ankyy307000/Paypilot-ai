package com.paypilot.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class RefundAction {

    public enum Status {
        PENDING_APPROVAL,
        PROCESSING,
        COMPLETED,
        FAILED,
        REJECTED
    }

    private final String id;
    private final String captureId;
    private final BigDecimal amount;
    private final String currency;
    private final Instant createdAt;

    private Status status;
    private String paypalRefundId;

    public RefundAction(
            String captureId,
            BigDecimal amount,
            String currency) {

        this(UUID.randomUUID().toString(),
                captureId, amount, currency,
                Instant.now(),
                Status.PENDING_APPROVAL,
                null);
    }

    // Restore an existing action from H2.
    public RefundAction(
            String id,
            String captureId,
            BigDecimal amount,
            String currency,
            Instant createdAt,
            Status status,
            String paypalRefundId) {

        this.id = id;
        this.captureId = captureId;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = createdAt;
        this.status = status;
        this.paypalRefundId = paypalRefundId;
    }

    public String getId() {
        return id;
    }

    public String getCaptureId() {
        return captureId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public synchronized Status getStatus() {
        return status;
    }

    public synchronized void setStatus(Status status) {
        this.status = status;
    }

    public synchronized String getPaypalRefundId() {
        return paypalRefundId;
    }

    public synchronized void setPaypalRefundId(String id) {
        this.paypalRefundId = id;
    }
}
