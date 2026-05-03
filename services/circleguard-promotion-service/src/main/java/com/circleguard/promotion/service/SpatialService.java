package com.circleguard.promotion.service;

import com.circleguard.promotion.model.*;
import com.circleguard.promotion.repository.jpa.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SpatialService {
    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final AccessPointRepository accessPointRepository;

    @Transactional
    public Building createBuilding(String name, String code, String description) {
        Building building = Building.builder()
                .name(name)
                .code(code)
                .description(description)
                .build();
        return buildingRepository.save(building);
    }

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

    public List<Building> getAllBuildings() {
        return buildingRepository.findAll();
    }

    public List<Floor> getFloorsByBuilding(UUID buildingId) {
        return floorRepository.findByBuildingId(buildingId);
    }

    @Transactional
    public Building updateBuilding(UUID id, String name, String code, String description) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Building not found"));
        building.setName(name);
        building.setCode(code);
        building.setDescription(description);
        return buildingRepository.save(building);
    }

    @Transactional
    public void deleteBuilding(UUID id) {
        if (!floorRepository.findByBuildingId(id).isEmpty()) {
            throw new RuntimeException("Cannot delete building with existing floors");
        }
        buildingRepository.deleteById(id);
    }

    @Transactional
    public Floor updateFloor(UUID id, Integer floorNumber, String name) {
        Floor floor = floorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Floor not found"));
        floor.setFloorNumber(floorNumber);
        floor.setName(name);
        return floorRepository.save(floor);
    }

    @Transactional
    public void deleteFloor(UUID id) {
        if (!accessPointRepository.findByFloorId(id).isEmpty()) {
            throw new RuntimeException("Cannot delete floor with existing access points");
        }
        floorRepository.deleteById(id);
    }

    @Transactional
    public AccessPoint registerAccessPoint(UUID floorId, String macAddress, Double x, Double y, String name) {
        Floor floor = floorRepository.findById(floorId)
                .orElseThrow(() -> new RuntimeException("Floor not found"));
        
        AccessPoint ap = AccessPoint.builder()
                .floor(floor)
                .macAddress(macAddress)
                .coordinateX(x)
                .coordinateY(y)
                .name(name)
                .build();
        return accessPointRepository.save(ap);
    }

    public Optional<AccessPoint> getAccessPoint(UUID id) {
        return accessPointRepository.findById(id);
    }

    public List<AccessPoint> getAccessPointsByFloor(UUID floorId) {
        return accessPointRepository.findByFloorId(floorId);
    }

    @Transactional
    public AccessPoint updateAccessPoint(UUID id, String macAddress, Double x, Double y, String name) {
        AccessPoint ap = accessPointRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Access Point not found"));
        ap.setMacAddress(macAddress);
        ap.setCoordinateX(x);
        ap.setCoordinateY(y);
        ap.setName(name);
        return accessPointRepository.save(ap);
    }

    @Transactional
    public void deleteAccessPoint(UUID id) {
        accessPointRepository.deleteById(id);
    }
}
