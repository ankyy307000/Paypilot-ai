package com.paypilot.controller;

import com.paypilot.model.Transaction;
import com.paypilot.service.TransactionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<Transaction> getTransactions() {
        return transactionService.getTransactions();
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {

        List<Transaction> transactions = transactionService.getTransactions();

        List<Transaction> completedPayments = transactions.stream()
                .filter(t -> "PAYMENT".equals(t.type()))
                .filter(t -> "COMPLETED".equals(t.status()))
                .toList();

        BigDecimal revenue = completedPayments.stream()
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "totalRevenue", revenue,
                "totalTransactions", completedPayments.size(),
                "totalRefunds", BigDecimal.ZERO,
                "netRevenue", revenue,
                "currency", "USD",
                "dataSource", "DEMO");
    }
}
