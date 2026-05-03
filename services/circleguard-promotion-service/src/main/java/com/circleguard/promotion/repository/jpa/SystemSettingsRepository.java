package com.circleguard.promotion.repository.jpa;

import com.circleguard.promotion.model.jpa.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {
    
    /**
     * Singleton management for system settings.
     * We only expect one row in this table.
     */
    @org.springframework.cache.annotation.Cacheable(value = "systemSettings")
    default Optional<SystemSettings> getSettings() {
        return findAll().stream().findFirst();
    }
}
