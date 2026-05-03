package com.circleguard.promotion.repository.jpa;

import com.circleguard.promotion.model.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, UUID> {
    Optional<Building> findByCode(String code);
}
