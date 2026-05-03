package com.circleguard.promotion.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "access_points")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mac_address", unique = true, nullable = false)
    private String macAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id")
    private Floor floor;

    @Column(name = "coordinate_x", nullable = false)
    private Double coordinateX;

    @Column(name = "coordinate_y", nullable = false)
    private Double coordinateY;

    private String name;
}
