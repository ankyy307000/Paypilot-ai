package com.paypilot.controller;

import com.paypilot.model.ActionPlan;
import com.paypilot.model.RefundAction;
import com.paypilot.service.ActionPlannerService;
import com.paypilot.service.RefundActionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AiRefundController {

    private final ActionPlannerService plannerService;
    private final RefundActionService refundActionService;

    public AiRefundController(
            ActionPlannerService plannerService,
            RefundActionService refundActionService) {
        this.plannerService = plannerService;
        this.refundActionService = refundActionService;
    }

    @PostMapping("/refund/propose")
    public RefundAction propose(
            @RequestBody Map<String, String> request) {

        String message = request.get("message");

        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message is required");
        }

        ActionPlan plan = plannerService.plan(message);

        if (!"REFUND".equals(plan.action())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only refund actions are supported");
        }

        if (!plan.requiresApproval()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Refund must require human approval");
        }

        if (plan.captureId() == null
                || plan.captureId().isBlank()
                || plan.amount() == null
                || plan.amount().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Refund requires a Capture ID and amount");
        }

        if (!"USD".equals(plan.currency())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only USD refunds are supported");
        }

        try {
            BigDecimal amount = new BigDecimal(plan.amount());

            return refundActionService.propose(
                    plan.captureId(),
                    amount,
                    plan.currency());

        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid refund amount");
        }
    }
}
