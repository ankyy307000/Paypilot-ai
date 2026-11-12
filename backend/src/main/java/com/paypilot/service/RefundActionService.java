package com.paypilot.service;

import com.paypilot.entity.RefundActionEntity;
import com.paypilot.model.RefundAction;
import com.paypilot.repository.RefundActionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RefundActionService {

    private final RefundActionRepository repository;
    private final PayPalRefundService paypalRefundService;

    // Prevent simultaneous operations within this backend instance.
    private final Map<String, Object> captureLocks = new ConcurrentHashMap<>();

    public RefundActionService(
            PayPalRefundService paypalRefundService,
            RefundActionRepository repository) {

        this.paypalRefundService = paypalRefundService;
        this.repository = repository;
    }

    private Object lockFor(String captureId) {
        return captureLocks.computeIfAbsent(
                captureId, key -> new Object());
    }

    private RefundActionEntity toEntity(RefundAction action) {
        RefundActionEntity entity = new RefundActionEntity();

        entity.setId(action.getId());
        entity.setCaptureId(action.getCaptureId());
        entity.setAmount(action.getAmount());
        entity.setCurrency(action.getCurrency());
        entity.setStatus(action.getStatus().name());
        entity.setPaypalRefundId(action.getPaypalRefundId());
        entity.setCreatedAt(action.getCreatedAt());

        return entity;
    }

    private RefundAction fromEntity(RefundActionEntity entity) {
        return new RefundAction(
                entity.getId(),
                entity.getCaptureId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getCreatedAt(),
                RefundAction.Status.valueOf(entity.getStatus()),
                entity.getPaypalRefundId());
    }

    private RefundAction save(RefundAction action) {
        return fromEntity(repository.saveAndFlush(toEntity(action)));
    }

    // A previous approval attempt permanently blocks further
    // refunds on this capture until manually reconciled.
    private boolean isBlocked(String captureId) {
        return repository.findByCaptureId(captureId)
                .stream()
                .anyMatch(a -> a.getStatus().equals("PROCESSING") ||
                        a.getStatus().equals("COMPLETED") ||
                        a.getStatus().equals("FAILED"));
    }

    private boolean hasActiveProposal(String captureId) {
        return repository.findByCaptureId(captureId)
                .stream()
                .anyMatch(a -> a.getStatus().equals("PENDING_APPROVAL"));
    }

    public RefundAction propose(
            String captureId,
            BigDecimal amount,
            String currency) {

        if (captureId == null || captureId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Capture ID is required");
        }

        final String normalizedCaptureId = captureId.trim();

        if (amount == null ||
                amount.signum() <= 0 ||
                amount.scale() > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid refund amount");
        }

        if (!"USD".equals(currency)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only USD is supported");
        }

        synchronized (lockFor(normalizedCaptureId)) {

            if (isBlocked(normalizedCaptureId)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Capture has a prior or uncertain refund");
            }

            if (hasActiveProposal(normalizedCaptureId)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "An active refund already exists");
            }

            return save(new RefundAction(
                    normalizedCaptureId,
                    amount,
                    currency));
        }
    }

    public RefundAction get(String id) {
        return repository.findById(id)
                .map(this::fromEntity)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Refund action not found"));
    }

    public List<RefundAction> history() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::fromEntity)
                .toList();
    }

    public RefundAction reject(String id) {
        RefundAction existing = get(id);

        synchronized (lockFor(existing.getCaptureId())) {
            RefundAction action = get(id);

            if (action.getStatus() != RefundAction.Status.PENDING_APPROVAL) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Refund is no longer pending");
            }

            action.setStatus(RefundAction.Status.REJECTED);
            return save(action);
        }
    }

    public RefundAction approve(String id) {
        RefundAction existing = get(id);
        final String captureId = existing.getCaptureId();

        RefundAction action;

        synchronized (lockFor(captureId)) {
            action = get(id);

            if (action.getStatus() != RefundAction.Status.PENDING_APPROVAL) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Refund is not awaiting approval");
            }

            if (isBlocked(captureId)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Capture is blocked from further refunds");
            }

            // Persist BEFORE making any PayPal request.
            // If the backend crashes, this capture stays blocked.
            action.setStatus(RefundAction.Status.PROCESSING);
            action = save(action);
        }

        try {
            Map<?, ?> capture = paypalRefundService.getCapture(captureId);

            if (capture == null ||
                    !"COMPLETED".equals(capture.get("status"))) {
                throw new IllegalStateException(
                        "PayPal capture is not completed");
            }

            Object rawAmount = capture.get("amount");

            if (!(rawAmount instanceof Map<?, ?> captureAmount)) {
                throw new IllegalStateException(
                        "PayPal capture amount is missing");
            }

            Object value = captureAmount.get("value");
            Object currency = captureAmount.get("currency_code");

            if (value == null || currency == null) {
                throw new IllegalStateException(
                        "Invalid PayPal capture amount");
            }

            BigDecimal capturedValue = new BigDecimal(value.toString());

            if (!action.getCurrency().equals(currency.toString())) {
                throw new IllegalStateException(
                        "Currency mismatch");
            }

            if (action.getAmount().compareTo(capturedValue) > 0) {
                throw new IllegalStateException(
                        "Refund exceeds original capture amount");
            }

            // Original capture amount is NOT the remaining
            // refundable balance. External refunds are not
            // visible in the local H2 ledger.
            Map<?, ?> result = paypalRefundService.refund(
                    captureId,
                    action.getAmount(),
                    action.getCurrency(),
                    action.getId());

            if (result == null || result.get("id") == null) {
                throw new IllegalStateException(
                        "PayPal refund response missing ID");
            }

            synchronized (lockFor(captureId)) {
                action.setPaypalRefundId(
                        result.get("id").toString());

                if ("COMPLETED".equals(
                        String.valueOf(result.get("status")))) {
                    action.setStatus(
                            RefundAction.Status.COMPLETED);
                }

                // Non-completed statuses remain PROCESSING
                // until separately reconciled.
                return save(action);
            }

        } catch (Exception ex) {
            // Preserve PROCESSING in H2.
            // Do not automatically retry an uncertain refund.
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Refund not confirmed. Verify PayPal " +
                            "before taking further action.",
                    ex);
        }
    }
}
