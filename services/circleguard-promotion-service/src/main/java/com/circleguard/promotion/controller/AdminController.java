package com.circleguard.promotion.controller;

import com.circleguard.promotion.model.jpa.SystemSettings;
import com.circleguard.promotion.repository.jpa.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final SystemSettingsRepository settingsRepository;

    @GetMapping
    @org.springframework.cache.annotation.Cacheable(value = "systemSettings")
    public ResponseEntity<SystemSettings> getSettings() {
        SystemSettings settings = settingsRepository.getSettings()
            .orElseGet(this::initializeDefaultSettings);
        return ResponseEntity.ok(settings);
    }

    @PostMapping
    @org.springframework.cache.annotation.CacheEvict(value = "systemSettings", allEntries = true)
    public ResponseEntity<SystemSettings> updateSettings(@RequestBody SystemSettings newSettings) {
        log.info("Updating system settings: {}", newSettings);
        
        SystemSettings settings = settingsRepository.getSettings()
            .orElseGet(this::initializeDefaultSettings);
            
        if (newSettings.getUnconfirmedFencingEnabled() != null) {
            settings.setUnconfirmedFencingEnabled(newSettings.getUnconfirmedFencingEnabled());
        }
        if (newSettings.getAutoThresholdSeconds() != null) {
            settings.setAutoThresholdSeconds(newSettings.getAutoThresholdSeconds());
        }
        if (newSettings.getMandatoryFenceDays() != null) {
            settings.setMandatoryFenceDays(newSettings.getMandatoryFenceDays());
        }
        if (newSettings.getEncounterWindowDays() != null) {
            settings.setEncounterWindowDays(newSettings.getEncounterWindowDays());
        }
        
        settingsRepository.save(settings);
        return ResponseEntity.ok(settings);
    }

    @PostMapping("/toggle-unconfirmed-fencing")
    @org.springframework.cache.annotation.CacheEvict(value = "systemSettings", allEntries = true)
    public ResponseEntity<SystemSettings> toggleUnconfirmedFencing(@RequestParam boolean enabled) {
        log.info("Health Authority toggle: Unconfirmed Fencing -> {}", enabled);
        
        SystemSettings settings = settingsRepository.getSettings()
            .orElseGet(this::initializeDefaultSettings);
            
        settings.setUnconfirmedFencingEnabled(enabled);
        settingsRepository.save(settings);
        
        return ResponseEntity.ok(settings);
    }

    private SystemSettings initializeDefaultSettings() {
        log.info("Initializing default system settings...");
        SystemSettings defaults = SystemSettings.builder()
            .unconfirmedFencingEnabled(true)
            .autoThresholdSeconds(3600L) // 1 Hour
            .mandatoryFenceDays(14)
            .encounterWindowDays(14)
            .build();
        return settingsRepository.save(defaults);
    }
}
