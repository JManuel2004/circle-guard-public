package com.circleguard.promotion.controller;

import com.circleguard.promotion.service.HealthStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthStatusController {
    private final HealthStatusService statusService;

    @PostMapping("/report")
    @PreAuthorize("hasRole('HEALTH_CENTER')")
    public ResponseEntity<Void> reportStatus(@RequestBody Map<String, Object> request) {
        String anonymousId = (String) request.get("anonymousId");
        String status = (String) request.get("status");
        boolean override = request.containsKey("adminOverride") && (boolean) request.get("adminOverride");
        
        statusService.updateStatus(anonymousId, status, override);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirmed")
    @PreAuthorize("hasRole('HEALTH_CENTER')")
    public ResponseEntity<Void> confirmPositive(@RequestBody Map<String, String> request) {
        String anonymousId = request.get("anonymousId");
        statusService.updateStatus(anonymousId, "CONFIRMED");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/recovery/{id}")
    @PreAuthorize("hasRole('HEALTH_CENTER')")
    public ResponseEntity<Void> recover(@PathVariable String id) {
        statusService.promoteToRecovered(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resolve")
    @PreAuthorize("hasRole('HEALTH_CENTER')")
    public ResponseEntity<Void> resolve(@RequestBody Map<String, Object> request) {
        String anonymousId = (String) request.get("anonymousId");
        boolean override = request.containsKey("adminOverride") && (boolean) request.get("adminOverride");

        statusService.resolveStatus(anonymousId, override);
        return ResponseEntity.ok().build();
    }
}
