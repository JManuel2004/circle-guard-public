package com.circleguard.promotion.controller;

import com.circleguard.promotion.repository.graph.UserNodeRepository;
import com.circleguard.promotion.service.AutoCircleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/encounters")
@RequiredArgsConstructor
@Slf4j
public class EncounterController {

    private final UserNodeRepository userRepository;
    private final AutoCircleService autoCircleService;

    @Data
    public static class EncounterRequest {
        private String sourceId;
        private String targetId;
        private String locationId;
    }

    @PostMapping("/report")
    public ResponseEntity<Void> reportEncounter(@RequestBody EncounterRequest request) {
        log.info("Reporting encounter: {} -> {} at {}", 
            request.getSourceId(), request.getTargetId(), request.getLocationId());
        
        userRepository.recordEncounter(
            request.getSourceId(),
            request.getTargetId(),
            System.currentTimeMillis(),
            request.getLocationId() != null ? request.getLocationId() : "mobile_ble"
        );

        autoCircleService.evaluateEncounter(request.getSourceId(), request.getTargetId());
        
        return ResponseEntity.ok().build();
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{id}/validity")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('HEALTH_CENTER')")
    public ResponseEntity<Void> toggleValidity(@org.springframework.web.bind.annotation.PathVariable Long id) {
        log.info("Toggling validity for encounter relationship: {}", id);
        userRepository.toggleEncounterValidity(id);
        return ResponseEntity.ok().build();
    }

    @org.springframework.web.bind.annotation.PostMapping("/{id}/force-fence")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('HEALTH_CENTER')")
    public ResponseEntity<Void> forceFence(@org.springframework.web.bind.annotation.PathVariable Long id) {
        log.info("Force fencing encounter relationship: {}", id);
        userRepository.forceEncounterFence(id);
        return ResponseEntity.ok().build();
    }
}
