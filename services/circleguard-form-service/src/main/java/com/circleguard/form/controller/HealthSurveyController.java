package com.circleguard.form.controller;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.service.HealthSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/surveys")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class HealthSurveyController {
    private final HealthSurveyService surveyService;

    @PostMapping
    public ResponseEntity<HealthSurvey> submit(@RequestBody HealthSurvey survey) {
        return ResponseEntity.ok(surveyService.submitSurvey(survey));
    }
}
