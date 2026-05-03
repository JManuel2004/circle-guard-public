package com.circleguard.promotion.model.jpa;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unconfirmed_fencing_enabled", nullable = false)
    private Boolean unconfirmedFencingEnabled;

    @Column(name = "auto_threshold_seconds", nullable = false)
    private Long autoThresholdSeconds;

    @Column(name = "mandatory_fence_days", nullable = false)
    private Integer mandatoryFenceDays;

    @Column(name = "encounter_window_days", nullable = false)
    private Integer encounterWindowDays;
}
