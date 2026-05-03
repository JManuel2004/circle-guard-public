package com.circleguard.form.controller;

import com.circleguard.form.model.ValidationStatus;
import com.circleguard.form.service.HealthSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CertificateValidationController {

    private final HealthSurveyService surveyService;

    @GetMapping("/pending")
    public ResponseEntity<?> getPending() {
        return ResponseEntity.ok(surveyService.getPendingSurveys());
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<?> validate(
            @PathVariable UUID id,
            @RequestParam ValidationStatus status,
            @RequestParam UUID adminId) {
        surveyService.validateSurvey(id, status, adminId);
        return ResponseEntity.ok().build();
    }
}
