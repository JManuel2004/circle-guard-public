package com.circleguard.promotion.controller;

import com.circleguard.promotion.dto.AccessPointDTO;
import com.circleguard.promotion.model.AccessPoint;
import com.circleguard.promotion.service.AccessPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/access-points")
@RequiredArgsConstructor
public class AccessPointController {
    private final AccessPointService accessPointService;

    @GetMapping("/{id}")
    public ResponseEntity<AccessPointDTO> getAccessPoint(@PathVariable UUID id) {
        return accessPointService.getAccessPoint(id)
                .map(this::convertApToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<AccessPointDTO> updateAccessPoint(@PathVariable UUID id, @RequestBody AccessPointDTO request) {
        AccessPoint ap = accessPointService.updateAccessPoint(
                id,
                request.getMacAddress(),
                request.getCoordinateX(),
                request.getCoordinateY(),
                request.getName()
        );
        return ResponseEntity.ok(convertApToDTO(ap));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteAccessPoint(@PathVariable UUID id) {
        accessPointService.deleteAccessPoint(id);
        return ResponseEntity.ok().build();
    }

    private AccessPointDTO convertApToDTO(AccessPoint ap) {
        return AccessPointDTO.builder()
                .id(ap.getId())
                .macAddress(ap.getMacAddress())
                .floorId(ap.getFloor() != null ? ap.getFloor().getId() : null)
                .coordinateX(ap.getCoordinateX())
                .coordinateY(ap.getCoordinateY())
                .name(ap.getName())
                .build();
    }
}
