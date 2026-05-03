package com.circleguard.promotion.service;

import com.circleguard.promotion.model.AccessPoint;
import com.circleguard.promotion.model.Floor;
import com.circleguard.promotion.repository.jpa.AccessPointRepository;
import com.circleguard.promotion.repository.jpa.FloorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessPointService {
    private final AccessPointRepository accessPointRepository;
    private final FloorRepository floorRepository;

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
