package com.circleguard.promotion.listener;

import com.circleguard.promotion.service.HealthStatusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * Validates the certificate.validated → status routing logic inside SurveyListener.
 * The existing SurveyListenerTest only covers the survey.submitted path.
 */
@ExtendWith(MockitoExtension.class)
class CertificateValidationRouteTest {

    @Mock
    private HealthStatusService healthStatusService;

    @InjectMocks
    private SurveyListener surveyListener;

    // An APPROVED certificate must reinstate the user to ACTIVE
    @Test
    void onCertificateValidated_WhenStatusApproved_CallsUpdateStatusToActive() {
        Map<String, Object> event = Map.of(
                "anonymousId", "user-cleared-123",
                "status",      "APPROVED",
                "adminId",     "admin-uuid",
                "timestamp",   System.currentTimeMillis()
        );

        surveyListener.onCertificateValidated(event);

        verify(healthStatusService).updateStatus("user-cleared-123", "ACTIVE");
    }

    // A REJECTED certificate must not alter the user's current status
    @Test
    void onCertificateValidated_WhenStatusRejected_DoesNotChangeStatus() {
        Map<String, Object> event = Map.of(
                "anonymousId", "user-rejected-456",
                "status",      "REJECTED",
                "adminId",     "admin-uuid",
                "timestamp",   System.currentTimeMillis()
        );

        surveyListener.onCertificateValidated(event);

        verify(healthStatusService, never()).updateStatus(anyString(), anyString());
    }

    // A null anonymousId must not trigger any status change (defensive guard)
    @Test
    void onCertificateValidated_WhenAnonymousIdNull_DoesNotThrowAndSkipsUpdate() {
        Map<String, Object> event = new java.util.HashMap<>();
        event.put("anonymousId", null);
        event.put("status",      "APPROVED");

        surveyListener.onCertificateValidated(event);

        verifyNoInteractions(healthStatusService);
    }
}
