package com.paypilot.controller;

import com.paypilot.service.AgentOrchestratorService;
import com.paypilot.service.AgentOrchestratorService.OrchestratorResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentOrchestratorController {

    private final AgentOrchestratorService orchestrator;

    public AgentOrchestratorController(
            AgentOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/orchestrate")
    public OrchestratorResponse orchestrate(
            @RequestBody Map<String, String> request) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Request body is required");
        }

        return orchestrator.orchestrate(
                request.get("message"));
    }
}
