package com.circleguard.notification.service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for synchronizing health status with the University LMS.
 */
public interface LmsService {
    /**
     * Synchronizes a student's attendance status to 'Remote' in the LMS.
     * 
     * @param userId The anonymousId of the student.
     * @param status The current health status.
     * @return A future that completes when the synchronization is done.
     */
    CompletableFuture<Void> syncRemoteAttendance(String userId, String status);
}
