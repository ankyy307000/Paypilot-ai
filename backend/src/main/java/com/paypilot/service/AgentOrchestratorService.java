package com.paypilot.service;

import com.paypilot.model.ActionPlan;
import com.paypilot.model.RefundAction;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class AgentOrchestratorService {

    private final IntentRouterService intentRouter;
    private final FinanceAnalystService financeAnalyst;
    private final ActionPlannerService actionPlanner;
    private final RefundActionService refundService;

    public AgentOrchestratorService(
            IntentRouterService intentRouter,
            FinanceAnalystService financeAnalyst,
            ActionPlannerService actionPlanner,
            RefundActionService refundService) {

        this.intentRouter = intentRouter;
        this.financeAnalyst = financeAnalyst;
        this.actionPlanner = actionPlanner;
        this.refundService = refundService;
    }

    public OrchestratorResponse orchestrate(String message) {

        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message is required");
        }

        IntentRouterService.Intent intent = intentRouter.classify(message);

        return switch (intent) {

            case ANALYZE -> new OrchestratorResponse(
                    "ANALYZE",
                    financeAnalyst.analyze(message),
                    null);

            case REFUND -> handleRefund(message);

            case UNKNOWN -> new OrchestratorResponse(
                    "UNKNOWN",
                    "I can analyze your finances and prepare "
                            + "refund requests for approval. "
                            + "Other financial actions are not "
                            + "supported yet.",
                    null);
        };
    }

    private OrchestratorResponse handleRefund(String message) {

        ActionPlan plan = actionPlanner.plan(message);

        if (!"REFUND".equals(plan.action())
                || !plan.requiresApproval()) {

            return new OrchestratorResponse(
                    "REFUND",
                    "I couldn't safely prepare this refund. "
                            + "Please provide a valid refund request.",
                    null);
        }

        if (plan.captureId() == null
                || plan.captureId().isBlank()
                || plan.amount() == null
                || plan.amount().isBlank()) {

            return new OrchestratorResponse(
                    "REFUND",
                    "Please provide the PayPal Capture ID "
                            + "and refund amount.",
                    null);
        }

        if (!"USD".equals(plan.currency())) {
            return new OrchestratorResponse(
                    "REFUND",
                    "Only USD refunds are currently supported.",
                    null);
        }

        final BigDecimal amount;

        try {
            amount = new BigDecimal(plan.amount());
        } catch (NumberFormatException ex) {
            return new OrchestratorResponse(
                    "REFUND",
                    "The refund amount is invalid.",
                    null);
        }

        RefundAction action = refundService.propose(
                plan.captureId(),
                amount,
                plan.currency());

        return new OrchestratorResponse(
                "REFUND",
                "I've prepared a refund proposal. "
                        + "Review the details before approving. "
                        + "No money has been refunded yet.",
                action);
    }

    public record OrchestratorResponse(
            String intent,
            String message,
            RefundAction refundAction) {
    }
}
