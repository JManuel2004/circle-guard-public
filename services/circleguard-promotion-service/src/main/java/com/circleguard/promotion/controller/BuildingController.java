package com.circleguard.promotion.controller;

import com.circleguard.promotion.dto.BuildingDTO;
import com.circleguard.promotion.dto.FloorDTO;
import com.circleguard.promotion.model.Building;
import com.circleguard.promotion.model.Floor;
import com.circleguard.promotion.service.BuildingService;
import com.circleguard.promotion.service.FloorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/buildings")
@RequiredArgsConstructor
public class BuildingController {
    private final BuildingService buildingService;
    private final FloorService floorService;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BuildingDTO> createBuilding(@RequestBody BuildingDTO request) {
        Building building = buildingService.createBuilding(
                request.getName(),
                request.getCode(),
                request.getDescription(),
                request.getLatitude(),
                request.getLongitude(),
                request.getAddress()
        );
        return ResponseEntity.ok(convertToDTO(building));
    }

    @GetMapping
    public ResponseEntity<List<BuildingDTO>> listBuildings() {
        List<BuildingDTO> dtos = buildingService.getAllBuildings().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}/floors")
    public ResponseEntity<List<FloorDTO>> getFloors(@PathVariable UUID id) {
        List<FloorDTO> dtos = floorService.getFloorsByBuilding(id).stream()
                .map(this::convertFloorToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{id}/floors")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<FloorDTO> addFloor(@PathVariable UUID id, @RequestBody FloorDTO request) {
        Floor floor = floorService.addFloor(
                id,
                request.getFloorNumber(),
                request.getName()
        );
        return ResponseEntity.ok(convertFloorToDTO(floor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BuildingDTO> updateBuilding(@PathVariable UUID id, @RequestBody BuildingDTO request) {
        Building building = buildingService.updateBuilding(
                id,
                request.getName(),
                request.getCode(),
                request.getDescription(),
                request.getLatitude(),
                request.getLongitude(),
                request.getAddress()
        );
        return ResponseEntity.ok(convertToDTO(building));
    }

    private BuildingDTO convertToDTO(Building building) {
        return BuildingDTO.builder()
                .id(building.getId())
                .name(building.getName())
                .code(building.getCode())
                .description(building.getDescription())
                .latitude(building.getLatitude())
                .longitude(building.getLongitude())
                .address(building.getAddress())
                .floors(building.getFloors() != null ? 
                    building.getFloors().stream().map(this::convertFloorToDTO).collect(Collectors.toList()) : null)
                .build();
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBuilding(@PathVariable UUID id) {
        buildingService.deleteBuilding(id);
        return ResponseEntity.ok().build();
    }
}
