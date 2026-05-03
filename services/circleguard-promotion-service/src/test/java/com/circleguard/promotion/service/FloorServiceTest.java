package com.circleguard.promotion.service;

import com.circleguard.promotion.model.Floor;
import com.circleguard.promotion.repository.jpa.AccessPointRepository;
import com.circleguard.promotion.repository.jpa.BuildingRepository;
import com.circleguard.promotion.repository.jpa.FloorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FloorServiceTest {

    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private FloorRepository floorRepository;
    @Mock
    private AccessPointRepository accessPointRepository;

    private FloorService floorService;

    @BeforeEach
    void setUp() {
        floorService = new FloorService(buildingRepository, floorRepository, accessPointRepository);
    }

    @Test
    void updateFloor_ShouldUpdateFloorPlanUrl() {
        // Arrange
        UUID floorId = UUID.randomUUID();
        String oldUrl = "http://old.url";
        String newUrl = "http://new.url";
        
        Floor floor = Floor.builder()
                .id(floorId)
                .name("Level 1")
                .floorNumber(1)
                .floorPlanUrl(oldUrl)
                .build();

        when(floorRepository.findById(floorId)).thenReturn(Optional.of(floor));
        when(floorRepository.save(any(Floor.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Floor updated = floorService.updateFloor(floorId, null, null, newUrl);

        // Assert
        assertEquals(newUrl, updated.getFloorPlanUrl());
        assertEquals("Level 1", updated.getName()); // Should remain unchanged
        verify(floorRepository).save(floor);
    }

    @Test
    void updateFloor_ShouldNotUpdateIfUrlIsNull() {
        // Arrange
        UUID floorId = UUID.randomUUID();
        String oldUrl = "http://old.url";
        
        Floor floor = Floor.builder()
                .id(floorId)
                .floorPlanUrl(oldUrl)
                .build();

        when(floorRepository.findById(floorId)).thenReturn(Optional.of(floor));
        when(floorRepository.save(any(Floor.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Floor updated = floorService.updateFloor(floorId, null, null, null);

        // Assert
        assertEquals(oldUrl, updated.getFloorPlanUrl());
        verify(floorRepository).save(floor);
    }
}
