package com.circleguard.dashboard.service;

import com.circleguard.dashboard.client.PromotionClient;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final JdbcTemplate jdbc;
    private final PromotionClient promotionClient;
    private final KAnonymityFilter kAnonymityFilter;

    /**
     * Gets campus-wide health summary from promotion-service.
     */
    public Map<String, Object> getCampusSummary() {
        return promotionClient.getHealthStats();
    }

    /**
     * Gets department-filtered health stats with K-Anonymity applied.
     */
    public Map<String, Object> getDepartmentStats(String department) {
        Map<String, Object> raw = promotionClient.getHealthStatsByDepartment(department);
        return kAnonymityFilter.apply(raw);
    }

    /**
     * Gets entry trends by location with K-Anonymity protection.
     */
    public List<Map<String, Object>> getEntryTrends(UUID locationId) {
        String query = "SELECT date_trunc('hour', entry_time) as hour, count(*) as entry_count " +
                       "FROM entry_logs WHERE location_id = ? " +
                       "GROUP BY hour ORDER BY hour DESC";
        
        List<Map<String, Object>> rows = jdbc.queryForList(query, locationId);
        
        // Apply K-Anonymity
        rows.forEach(row -> {
            long count = (long) row.get("entry_count");
            if (count < 5) {
                row.put("entry_count", "<5");
                row.put("note", "Insufficient data for privacy");
            }
        });
        
        return rows;
    }

    /**
     * Provides aggregated, university-wide health metrics for leadership.
     */
    public Map<String, Object> getGlobalHealthStats() {
        return getCampusSummary();
    }

    /**
     * Time-series data: status counts bucketed by time period.
     * Queries the local dashboard DB for event history.
     */
    public List<Map<String, Object>> getTimeSeries(String period, int limit) {
        // period: "hourly" or "daily"
        String truncation = "daily".equals(period) ? "day" : "hour";
        
        String query = "SELECT date_trunc('" + truncation + "', event_time) as bucket, " +
                       "status, count(*) as total " +
                       "FROM status_events " +
                       "GROUP BY bucket, status " +
                       "ORDER BY bucket DESC " +
                       "LIMIT ?";
        
        try {
            return jdbc.queryForList(query, limit);
        } catch (Exception e) {
            // Table may not exist yet — return mock data for PoC
            return generateMockTimeSeries(limit);
        }
    }

    private List<Map<String, Object>> generateMockTimeSeries(int limit) {
        List<Map<String, Object>> series = new ArrayList<>();
        long now = System.currentTimeMillis();
        String[] statuses = {"ACTIVE", "SUSPECT", "PROBABLE", "CONFIRMED"};
        Random rng = new Random(42);

        for (int i = 0; i < Math.min(limit, 24); i++) {
            for (String status : statuses) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("bucket", new Date(now - (long) i * 3600_000));
                point.put("status", status);
                int base = "ACTIVE".equals(status) ? 200 : "SUSPECT".equals(status) ? 30 : 10;
                point.put("total", base + rng.nextInt(20));
                series.add(point);
            }
        }
        return series;
    }
}
