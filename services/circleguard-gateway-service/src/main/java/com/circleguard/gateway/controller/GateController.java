package com.circleguard.gateway.controller;

import com.circleguard.gateway.service.QrValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/gate")
@RequiredArgsConstructor
public class GateController {
    private final QrValidationService validationService;

    @PostMapping("/validate")
    public ResponseEntity<QrValidationService.ValidationResult> validate(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        return ResponseEntity.ok(validationService.validateToken(token));
    }
}
