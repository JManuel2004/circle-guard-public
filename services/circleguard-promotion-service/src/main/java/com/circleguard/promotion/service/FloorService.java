package com.circleguard.promotion.service;

import com.circleguard.promotion.model.Building;
import com.circleguard.promotion.model.Floor;
import com.circleguard.promotion.repository.jpa.AccessPointRepository;
import com.circleguard.promotion.repository.jpa.BuildingRepository;
import com.circleguard.promotion.repository.jpa.FloorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FloorService {
    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final AccessPointRepository accessPointRepository;

    @Transactional
    public Floor addFloor(UUID buildingId, Integer floorNumber, String name) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new RuntimeException("Building not found"));
        
        Floor floor = Floor.builder()
                .building(building)
                .floorNumber(floorNumber)
                .name(name)
                .build();
        return floorRepository.save(floor);
    }

    public List<Floor> getFloorsByBuilding(UUID buildingId) {
        return floorRepository.findByBuildingId(buildingId);
    }

    @Transactional
    public Floor updateFloor(UUID id, Integer floorNumber, String name, String floorPlanUrl) {
        Floor floor = floorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Floor not found"));
        if (floorNumber != null) floor.setFloorNumber(floorNumber);
        if (name != null) floor.setName(name);
        if (floorPlanUrl != null) floor.setFloorPlanUrl(floorPlanUrl);
        return floorRepository.save(floor);
    }

    @Transactional
    public void deleteFloor(UUID id) {
        if (!accessPointRepository.findByFloorId(id).isEmpty()) {
            throw new RuntimeException("Cannot delete floor with existing access points");
        }
        floorRepository.deleteById(id);
    }
}
