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
import static org.mockito.Mockito.verify;

/**
 * Integration test: validates the Kafka pipeline from form-service to promotion-service
 * through the survey.submitted topic.
 *
 * A real embedded Kafka broker is used so that the full message-passing path
 * (serialization → broker → deserialization → @KafkaListener dispatch) is exercised.
 * HealthStatusService is mocked to isolate the Neo4j/Redis infrastructure.
 */
@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer"
})
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"survey.submitted"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@DirtiesContext
class SurveyToSuspectKafkaIT {

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

    @Test
    void surveyWithSymptoms_ProducedToKafka_SurveyListenerPromotesToSuspect() {
        String anonymousId = "kafka-it-user-" + System.currentTimeMillis();
        Map<String, Object> event = Map.of(
                "anonymousId", anonymousId,
                "hasSymptoms", true,
                "timestamp",   System.currentTimeMillis()
        );

        kafkaTemplate.send("survey.submitted", anonymousId, event);

        // Wait up to 10 s for the @KafkaListener to consume and route the message
        await().atMost(30, TimeUnit.SECONDS)
               .untilAsserted(() ->
                       verify(healthStatusService).updateStatus(anonymousId, "SUSPECT"));
    }

    @Test
    void surveyWithoutSymptoms_ProducedToKafka_SurveyListenerIgnoresEvent() throws InterruptedException {
        String anonymousId = "kafka-it-healthy-" + System.currentTimeMillis();
        Map<String, Object> event = Map.of(
                "anonymousId", anonymousId,
                "hasSymptoms", false,
                "timestamp",   System.currentTimeMillis()
        );

        kafkaTemplate.send("survey.submitted", anonymousId, event);

        // Short wait to let the consumer pick it up, then assert no status change
        TimeUnit.SECONDS.sleep(3);
        Mockito.verify(healthStatusService, Mockito.never()).updateStatus(anonymousId, "SUSPECT");
    }
}
