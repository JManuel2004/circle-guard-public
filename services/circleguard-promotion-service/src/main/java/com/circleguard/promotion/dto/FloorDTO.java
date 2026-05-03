package com.circleguard.promotion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FloorDTO {
    private UUID id;
    private UUID buildingId;
    private Integer floorNumber;
    private String name;
    private String floorPlanUrl;
    private List<AccessPointDTO> accessPoints;
}
