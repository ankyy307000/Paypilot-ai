package com.paypilot.service;

import com.paypilot.model.Transaction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    public List<Transaction> getTransactions() {
        // Demo data based on completed Sandbox payments.
        // These are NOT fetched from the PayPal API.
        return List.of(
                new Transaction(
                        "DEMO-TXN-001",
                        "John Doe",
                        new BigDecimal("120.00"),
                        "USD",
                        "PAYMENT",
                        "COMPLETED",
                        LocalDateTime.of(2026, 11, 11, 23, 45)),
                new Transaction(
                        "DEMO-TXN-002",
                        "John Doe",
                        new BigDecimal("75.00"),
                        "USD",
                        "PAYMENT",
                        "COMPLETED",
                        LocalDateTime.of(2026, 11, 11, 23, 47)),
                new Transaction(
                        "DEMO-TXN-003",
                        "John Doe",
                        new BigDecimal("200.00"),
                        "USD",
                        "PAYMENT",
                        "COMPLETED",
                        LocalDateTime.of(2026, 11, 11, 23, 48)));
    }
}
