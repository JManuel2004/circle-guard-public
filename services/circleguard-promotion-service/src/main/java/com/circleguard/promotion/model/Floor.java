package com.circleguard.promotion.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.util.List;

@Entity
@Table(name = "floors", uniqueConstraints = {@UniqueConstraint(columnNames = {"building_id", "floor_number"})})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Floor {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;

    @Column(name = "floor_number", nullable = false)
    private Integer floorNumber;

    private String name;

    @Column(name = "floor_plan_url")
    private String floorPlanUrl;

    @OneToMany(mappedBy = "floor", cascade = CascadeType.ALL)
    private List<AccessPoint> accessPoints;
}
