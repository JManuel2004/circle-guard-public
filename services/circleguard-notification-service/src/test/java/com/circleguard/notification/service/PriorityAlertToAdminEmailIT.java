package com.circleguard.notification.service;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

/**
 * Integration test: validates the three-hop chain
 *   promotion-service  →  (Kafka: alert.priority)
 *   notification-service  →  (HTTP GET: auth-service /permissions/alert:receive_priority)
 *   notification-service  →  templateService.generateEmailContent()
 *
 * WireMock intercepts the HTTP call to auth-service (hardcoded to localhost:8180).
 * EmbeddedKafka provides a real broker for the alert.priority consumer.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "auth.api.url=http://localhost:8180")
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"alert.priority"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@DirtiesContext
class PriorityAlertToAdminEmailIT {

    // Must start before Spring context — static field guarantees this
    @RegisterExtension
    static WireMockExtension authServiceMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(8180))
            .build();

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    // Stub the outbound channels so they don't make real network calls
    @MockBean private TemplateService templateService;
    @MockBean private EmailService emailService;
    @MockBean private SmsService smsService;
    @MockBean private PushService pushService;
    @MockBean private LmsService lmsService;
    @MockBean private org.springframework.mail.javamail.JavaMailSender mailSender;
    @MockBean private org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder;

    @Test
    void confirmedCaseAlert_KafkaFlow_CallsAuthServiceAndDispatchesEmailToAdmins() {
        // Stub auth-service: returns two admins with alert:receive_priority
        authServiceMock.stubFor(get(urlEqualTo("/api/v1/users/permissions/alert:receive_priority"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[" +
                                "{\"email\":\"admin1@university.edu\",\"username\":\"admin1\"}," +
                                "{\"email\":\"admin2@university.edu\",\"username\":\"admin2\"}" +
                                "]")));

        String payload = "{\"eventType\":\"CONFIRMED_CASE\",\"affectedCount\":1," +
                "\"anonymousId\":\"user-xyz\",\"timestamp\":" + System.currentTimeMillis() + "}";

        kafkaTemplate.send("alert.priority", payload);

        // 1. auth-service endpoint must have been called exactly once
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                authServiceMock.verify(1,
                        getRequestedFor(urlEqualTo(
                                "/api/v1/users/permissions/alert:receive_priority"))));

        // 2. Template generation triggered for each admin found
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(templateService).generateEmailContent("CONFIRMED_CASE", "admin1");
            verify(templateService).generateEmailContent("CONFIRMED_CASE", "admin2");
        });
    }

    @Test
    void priorityAlert_WhenNoAdminsFound_DoesNotCallTemplateService()
            throws InterruptedException {
        authServiceMock.stubFor(get(urlEqualTo("/api/v1/users/permissions/alert:receive_priority"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        String payload = "{\"eventType\":\"LARGE_OUTBREAK\",\"affectedCount\":25," +
                "\"anonymousId\":\"user-abc\",\"timestamp\":" + System.currentTimeMillis() + "}";

        kafkaTemplate.send("alert.priority", payload);

        TimeUnit.SECONDS.sleep(4);
        org.mockito.Mockito.verifyNoInteractions(templateService);
    }
}
