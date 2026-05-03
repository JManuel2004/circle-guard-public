package com.circleguard.notification.service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for managing external room reservations.
 */
public interface RoomReservationService {
    /**
     * Cancels a room reservation for a given circle and location.
     * 
     * @param circleId The ID of the circle (class) that is fenced.
     * @param locationId The ID of the location/room.
     * @return A future that completes when the cancellation is processed.
     */
    CompletableFuture<Void> cancelReservation(String circleId, String locationId);
}
