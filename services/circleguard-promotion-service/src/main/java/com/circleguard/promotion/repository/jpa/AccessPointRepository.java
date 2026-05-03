package com.circleguard.promotion.repository.jpa;

import com.circleguard.promotion.model.AccessPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

import java.util.List;

public interface AccessPointRepository extends JpaRepository<AccessPoint, UUID> {
    Optional<AccessPoint> findByMacAddress(String macAddress);
    List<AccessPoint> findByFloorId(UUID floorId);
    void deleteByFloorId(UUID floorId);
}
