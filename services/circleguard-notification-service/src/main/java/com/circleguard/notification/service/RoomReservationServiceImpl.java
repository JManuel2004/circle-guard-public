package com.circleguard.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class RoomReservationServiceImpl implements RoomReservationService {

    @Value("${room.booking.api.url:https://facilities.university.edu/api/v1/rooms}")
    private String roomBookingApiUrl;

    @Override
    @Async
    public CompletableFuture<Void> cancelReservation(String circleId, String locationId) {
        log.info("Room Cancellation: Automatically releasing reservation for location {} (Circle: {}) via API ({})", 
            locationId, circleId, roomBookingApiUrl);
        
        // Mock cancellation logic
        // webClient.post().uri(roomBookingApiUrl + "/" + locationId + "/cancel").bodyValue(Map.of("circleId", circleId, "reason", "FENCED_CLASS"))...

        return CompletableFuture.completedFuture(null);
    }
}
