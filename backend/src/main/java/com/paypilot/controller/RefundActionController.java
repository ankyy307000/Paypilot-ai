
package com.paypilot.controller;

import com.paypilot.model.RefundAction;
import com.paypilot.service.RefundActionService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/actions")
public class RefundActionController {

    private final RefundActionService service;

    public RefundActionController(RefundActionService service) {
        this.service = service;
    }

    @PostMapping("/refund/propose")
    public RefundAction propose(
            @RequestBody Map<String, String> request) {

        return service.propose(
                request.get("captureId"),
                new BigDecimal(request.get("amount")),
                request.get("currency"));
    }

    @GetMapping("/{id}")
    public RefundAction get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping("/{id}/reject")
    public RefundAction reject(@PathVariable String id) {
        return service.reject(id);
    }

    @PostMapping("/{id}/approve")
    public RefundAction approve(@PathVariable String id) {
        return service.approve(id);
    }

}
