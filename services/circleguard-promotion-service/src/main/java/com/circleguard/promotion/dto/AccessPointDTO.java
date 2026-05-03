package com.circleguard.promotion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessPointDTO {
    private UUID id;
    private String macAddress;
    private UUID floorId;
    private Double coordinateX;
    private Double coordinateY;
    private String name;
}
