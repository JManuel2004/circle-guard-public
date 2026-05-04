package com.circleguard.promotion.listener;

import com.circleguard.promotion.service.HealthStatusService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Integration test: validates the Kafka pipeline from form-service to promotion-service
 * through the certificate.validated topic (medical clearance flow).
 *
 * An APPROVED certificate must trigger updateStatus → ACTIVE.
 * A REJECTED certificate must produce no status change at all.
 */
@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer"
})
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"certificate.validated"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@DirtiesContext
class CertificateApprovedToActiveIT {

    @TestConfiguration
    static class TxConfig {
        @Bean @Primary
        public PlatformTransactionManager transactionManager() {
            return Mockito.mock(PlatformTransactionManager.class);
        }
        @Bean(name = "neo4jTransactionManager")
        public PlatformTransactionManager neo4jTransactionManager() {
            return Mockito.mock(PlatformTransactionManager.class);
        }
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean private HealthStatusService healthStatusService;
    @MockBean private Neo4jClient neo4jClient;
    @MockBean private StringRedisTemplate redisTemplate;
    @MockBean private org.springframework.cache.CacheManager cacheManager;
    @MockBean private com.circleguard.promotion.repository.graph.UserNodeRepository userNodeRepository;
    @MockBean private com.circleguard.promotion.repository.graph.CircleNodeRepository circleNodeRepository;
    @MockBean private com.circleguard.promotion.repository.jpa.SystemSettingsRepository systemSettingsRepository;

    // Medical clearance (APPROVED) must reinstate user to ACTIVE via Kafka
    @Test
    void approvedCertificate_ProducedToKafka_ListenerRestoresUserToActive() {
        String anonymousId = "cert-approved-" + System.currentTimeMillis();
        Map<String, Object> event = Map.of(
                "anonymousId", anonymousId,
                "status",      "APPROVED",
                "adminId",     "admin-uuid-001",
                "timestamp",   System.currentTimeMillis()
        );

        kafkaTemplate.send("certificate.validated", anonymousId, event);

        await().atMost(10, TimeUnit.SECONDS)
               .untilAsserted(() ->
                       verify(healthStatusService).updateStatus(anonymousId, "ACTIVE"));
    }

    // REJECTED certificate must pass through the listener without mutating any status
    @Test
    void rejectedCertificate_ProducedToKafka_ListenerDoesNotAlterStatus()
            throws InterruptedException {
        String anonymousId = "cert-rejected-" + System.currentTimeMillis();
        Map<String, Object> event = Map.of(
                "anonymousId", anonymousId,
                "status",      "REJECTED",
                "adminId",     "admin-uuid-002",
                "timestamp",   System.currentTimeMillis()
        );

        kafkaTemplate.send("certificate.validated", anonymousId, event);

        TimeUnit.SECONDS.sleep(3);
        verify(healthStatusService, never()).updateStatus(anonymousId, "ACTIVE");
    }
}
