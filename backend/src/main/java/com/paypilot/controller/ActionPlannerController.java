package com.paypilot.controller;

import com.paypilot.model.ActionPlan;
import com.paypilot.service.ActionPlannerService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class ActionPlannerController {

    private final ActionPlannerService plannerService;

    public ActionPlannerController(
            ActionPlannerService plannerService) {
        this.plannerService = plannerService;
    }

    @PostMapping("/plan")
    public ActionPlan plan(
            @RequestBody Map<String, String> request) {

        String message = request.get("message");

        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message is required");
        }

        return plannerService.plan(message);
    }
}
