package com.circleguard.promotion.service;

import com.circleguard.promotion.model.jpa.SystemSettings;
import com.circleguard.promotion.repository.jpa.SystemSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("test")
class StatusLifecycleTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public org.springframework.transaction.PlatformTransactionManager transactionManager() {
            return Mockito.mock(org.springframework.transaction.PlatformTransactionManager.class);
        }

        @Bean(name = "neo4jTransactionManager")
        public org.springframework.transaction.PlatformTransactionManager neo4jTransactionManager() {
            return Mockito.mock(org.springframework.transaction.PlatformTransactionManager.class);
        }
    }

    @Autowired
    private StatusLifecycleService lifecycleService;

    @MockBean
    private Neo4jClient neo4jClient;

    @MockBean
    private SystemSettingsRepository settingsRepository;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setup() {
        // Seed settings: 14 day mandatory fence
        SystemSettings settings = SystemSettings.builder()
                .unconfirmedFencingEnabled(true)
                .autoThresholdSeconds(3600L)
                .mandatoryFenceDays(14)
                .encounterWindowDays(14)
                .build();
        when(settingsRepository.getSettings()).thenReturn(Optional.of(settings));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void automaticTransition_ReleasesExpiredUsers() {
        // 1. Setup mock Neo4j response
        Neo4jClient.UnboundRunnableSpec runnableSpec = Mockito.mock(Neo4jClient.UnboundRunnableSpec.class, Mockito.RETURNS_DEEP_STUBS);
        when(neo4jClient.query(anyString())).thenReturn(runnableSpec);
        
        Map<String, Object> resultMap = Map.of(
            "releasedIds", List.of("EXPIRED_USER")
        );
        
        when(runnableSpec.bind(anyLong()).to(anyString())
                .fetch().one())
            .thenReturn(Optional.of(resultMap));

        // 2. Action: Run lifecycle processor
        lifecycleService.processAutomaticTransitions();

        // 3. Verify: Redis and Kafka updated
        verify(valueOperations).multiSet(ArgumentMatchers.anyMap());
        verify(kafkaTemplate).send(ArgumentMatchers.eq("promotion.status.changed"), ArgumentMatchers.eq("EXPIRED_USER"), ArgumentMatchers.anyMap());
    }

    @Test
    void automaticTransition_HandlesEmptyResults() {
        // 1. Setup mock Neo4j response with no released users
        Neo4jClient.UnboundRunnableSpec runnableSpec = Mockito.mock(Neo4jClient.UnboundRunnableSpec.class, Mockito.RETURNS_DEEP_STUBS);
        when(neo4jClient.query(anyString())).thenReturn(runnableSpec);
        
        Map<String, Object> resultMap = Map.of(
            "releasedIds", Collections.emptyList()
        );
        
        when(runnableSpec.bind(anyLong()).to(anyString())
                .fetch().one())
            .thenReturn(Optional.of(resultMap));

        // 2. Action: Run lifecycle processor
        lifecycleService.processAutomaticTransitions();

        // 3. Verify: No interactions with Redis/Kafka
        verify(valueOperations, Mockito.never()).multiSet(ArgumentMatchers.anyMap());
    }
}
