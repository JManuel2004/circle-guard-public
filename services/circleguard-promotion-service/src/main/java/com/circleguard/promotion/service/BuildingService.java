package com.circleguard.promotion.service;

import com.circleguard.promotion.model.Building;
import com.circleguard.promotion.repository.jpa.BuildingRepository;
import com.circleguard.promotion.repository.jpa.FloorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BuildingService {
    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;

    @Transactional
    public Building createBuilding(String name, String code, String description, Double latitude, Double longitude, String address) {
        Building building = Building.builder()
                .name(name)
                .code(code)
                .description(description)
                .latitude(latitude)
                .longitude(longitude)
                .address(address)
                .build();
        return buildingRepository.save(building);
    }

    public List<Building> getAllBuildings() {
        return buildingRepository.findAll();
    }

    @Transactional
    public Building updateBuilding(UUID id, String name, String code, String description, Double latitude, Double longitude, String address) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Building not found"));
        building.setName(name);
        building.setCode(code);
        building.setDescription(description);
        building.setLatitude(latitude);
        building.setLongitude(longitude);
        building.setAddress(address);
        return buildingRepository.save(building);
    }

    @Transactional
    public void deleteBuilding(UUID id) {
        if (!floorRepository.findByBuildingId(id).isEmpty()) {
            throw new RuntimeException("Cannot delete building with existing floors");
        }
        buildingRepository.deleteById(id);
    }
}
