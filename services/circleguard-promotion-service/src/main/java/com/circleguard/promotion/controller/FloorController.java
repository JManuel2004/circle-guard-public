package com.circleguard.promotion.controller;

import com.circleguard.promotion.dto.AccessPointDTO;
import com.circleguard.promotion.dto.FloorDTO;
import com.circleguard.promotion.model.AccessPoint;
import com.circleguard.promotion.model.Floor;
import com.circleguard.promotion.service.AccessPointService;
import com.circleguard.promotion.service.FloorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/floors")
@RequiredArgsConstructor
public class FloorController {
    private final FloorService floorService;
    private final AccessPointService accessPointService;

    @PostMapping("/{id}/access-points")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<AccessPointDTO> addAccessPoint(@PathVariable UUID id, @RequestBody AccessPointDTO request) {
        AccessPoint ap = accessPointService.registerAccessPoint(
                id,
                request.getMacAddress(),
                request.getCoordinateX(),
                request.getCoordinateY(),
                request.getName()
        );
        return ResponseEntity.ok(convertApToDTO(ap));
    }

    @GetMapping("/{id}/access-points")
    public ResponseEntity<List<AccessPointDTO>> getAccessPoints(@PathVariable UUID id) {
        List<AccessPointDTO> dtos = accessPointService.getAccessPointsByFloor(id).stream()
                .map(this::convertApToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<FloorDTO> updateFloor(@PathVariable UUID id, @RequestBody FloorDTO request) {
        Floor floor = floorService.updateFloor(
                id,
                request.getFloorNumber(),
                request.getName(),
                request.getFloorPlanUrl()
        );
        return ResponseEntity.ok(convertFloorToDTO(floor));
    }

    private FloorDTO convertFloorToDTO(Floor floor) {
        return FloorDTO.builder()
                .id(floor.getId())
                .buildingId(floor.getBuilding() != null ? floor.getBuilding().getId() : null)
                .floorNumber(floor.getFloorNumber())
                .name(floor.getName())
                .floorPlanUrl(floor.getFloorPlanUrl())
                .build();
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFloor(@PathVariable UUID id) {
        floorService.deleteFloor(id);
        return ResponseEntity.ok().build();
    }
}
