package com.circleguard.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class LmsServiceTest {

    @Autowired
    private LmsService lmsService;

    @Test
    void testRemoteAttendanceSync() {
        CompletableFuture<Void> future = lmsService.syncRemoteAttendance("student-123", "PROBABLE");
        future.join(); // Wait for completion
        assertThat(future).isCompleted();
    }
}
