package com.circleguard.promotion.service;

import com.circleguard.promotion.repository.graph.UserNodeRepository;
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
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import com.circleguard.promotion.exception.FenceException;
import com.circleguard.promotion.model.graph.UserNode;
import com.circleguard.promotion.model.jpa.SystemSettings;
import com.circleguard.promotion.repository.jpa.SystemSettingsRepository;
import java.util.Optional;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class HealthStatusServiceTest {

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
    private HealthStatusService healthStatusService;

    @MockBean
    private UserNodeRepository userNodeRepository;

    @MockBean
    private Neo4jClient neo4jClient;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private org.springframework.cache.CacheManager cacheManager;

    @MockBean
    private SystemSettingsRepository systemSettingsRepository;

    @MockBean
    private com.circleguard.promotion.repository.graph.CircleNodeRepository circleNodeRepository;

    @Test
    void shouldUpdateStatusSuccessfully() {
        String anonymousId = "user-abc-123";
        String status = "GREEN";

        // Mock Neo4j using Deep Stubs for the fluent API
        Neo4jClient.UnboundRunnableSpec runnableSpec = Mockito.mock(Neo4jClient.UnboundRunnableSpec.class, Mockito.RETURNS_DEEP_STUBS);
        when(neo4jClient.query(anyString())).thenReturn(runnableSpec);
        
        java.util.Map<String, Object> resultMap = new java.util.HashMap<>();
        resultMap.put("sourceId", anonymousId);
        resultMap.put("affectedContacts", java.util.Collections.emptyList());
        
        when(runnableSpec.bind(anyString()).to(anyString())
                .bind(anyString()).to(anyString())
                .bind(ArgumentMatchers.anyLong()).to(anyString())
                .fetch().one())
            .thenReturn(Optional.of(resultMap));

        // Mock Redis
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        assertDoesNotThrow(() -> healthStatusService.updateStatus(anonymousId, status));
        

        Mockito.verify(kafkaTemplate).send(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingStatusToActiveWithinFenceWindow() {
        String anonymousId = "user-fenced";
        
        // Mock user in SUSPECT status updated 5 days ago
        long fiveDaysAgo = System.currentTimeMillis() - (5L * 24 * 60 * 60 * 1000);
        UserNode user = UserNode.builder()
                .anonymousId(anonymousId)
                .status("SUSPECT")
                .statusUpdatedAt(fiveDaysAgo)
                .build();
        
        when(userNodeRepository.findById(anonymousId)).thenReturn(Optional.of(user));
        
        // Mock settings with 14 day mandatory fence
        SystemSettings settings = SystemSettings.builder()
                .mandatoryFenceDays(14)
                .build();
        when(systemSettingsRepository.getSettings()).thenReturn(Optional.of(settings));

        assertThrows(FenceException.class, () -> healthStatusService.resolveStatus(anonymousId));
    }

    @Test
    void shouldAllowOverrideWhenWithinFenceWindow() {
        String anonymousId = "user-fenced-override";
        
        long fiveDaysAgo = System.currentTimeMillis() - (5L * 24 * 60 * 60 * 1000);
        UserNode user = UserNode.builder()
                .anonymousId(anonymousId)
                .status("SUSPECT")
                .statusUpdatedAt(fiveDaysAgo)
                .build();
        
        when(userNodeRepository.findById(anonymousId)).thenReturn(Optional.of(user));
        
        SystemSettings settings = SystemSettings.builder()
                .mandatoryFenceDays(14)
                .build();
        when(systemSettingsRepository.getSettings()).thenReturn(Optional.of(settings));

        // Mock Neo4j
        Neo4jClient.UnboundRunnableSpec runnableSpec = Mockito.mock(Neo4jClient.UnboundRunnableSpec.class, Mockito.RETURNS_DEEP_STUBS);
        when(neo4jClient.query(anyString())).thenReturn(runnableSpec);
        
        // Mock Redis
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        assertDoesNotThrow(() -> healthStatusService.resolveStatus(anonymousId, true));
    }
}
