package com.paypilot.controller;

import com.paypilot.model.RefundAction;
import com.paypilot.service.RefundActionService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/actions")
public class RefundHistoryController {

    private final RefundActionService service;

    public RefundHistoryController(RefundActionService service) {
        this.service = service;
    }

    @GetMapping("/history")
    public List<RefundAction> history() {
        return service.history();
    }
}
