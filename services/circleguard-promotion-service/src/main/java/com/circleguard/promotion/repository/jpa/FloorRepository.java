package com.circleguard.promotion.repository.jpa;

import com.circleguard.promotion.model.Floor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface FloorRepository extends JpaRepository<Floor, UUID> {
    List<Floor> findByBuildingId(UUID buildingId);
}
