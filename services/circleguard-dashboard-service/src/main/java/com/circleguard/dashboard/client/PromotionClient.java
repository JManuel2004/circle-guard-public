package com.circleguard.dashboard.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@Slf4j
public class PromotionClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${circleguard.promotion-service.url:http://localhost:8088}")
    private String promotionServiceUrl;

    @SuppressWarnings("unchecked")
    public Map<String, Object> getHealthStats() {
        try {
            return restTemplate.getForObject(
                    promotionServiceUrl + "/api/v1/health-status/stats",
                    Map.class
            );
        } catch (Exception e) {
            log.error("Failed to fetch health stats from promotion-service", e);
            return Map.of("error", "Service unavailable", "timestamp", new Date());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getHealthStatsByDepartment(String department) {
        try {
            return restTemplate.getForObject(
                    promotionServiceUrl + "/api/v1/health-status/stats/department/" + department,
                    Map.class
            );
        } catch (Exception e) {
            log.error("Failed to fetch department stats from promotion-service", e);
            return Map.of("error", "Service unavailable", "department", department, "timestamp", new Date());
        }
    }
}
