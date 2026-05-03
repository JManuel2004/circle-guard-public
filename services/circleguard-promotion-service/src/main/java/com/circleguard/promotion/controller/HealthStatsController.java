package com.circleguard.promotion.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/health-status")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HealthStatsController {

    private final Neo4jClient neo4jClient;

    /**
     * Returns aggregated, anonymized counts of users by health status.
     * No individual IDs are exposed (FR-23).
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        String query = "MATCH (u:User) " +
                       "RETURN u.status AS status, count(u) AS total " +
                       "ORDER BY total DESC";

        Collection<Map<String, Object>> rows = neo4jClient.query(query)
                .fetch().all();

        Map<String, Object> result = new LinkedHashMap<>();
        long grandTotal = 0;

        for (Map<String, Object> row : rows) {
            String status = row.get("status") != null ? row.get("status").toString() : "UNKNOWN";
            long count = ((Number) row.get("total")).longValue();
            result.put(status.toLowerCase() + "Count", count);
            grandTotal += count;
        }

        result.put("totalUsers", grandTotal);
        result.put("timestamp", new Date());
        return ResponseEntity.ok(result);
    }

    /**
     * Returns status counts filtered by department/building.
     */
    @GetMapping("/stats/department/{department}")
    public ResponseEntity<Map<String, Object>> getStatsByDepartment(@PathVariable String department) {
        String query = "MATCH (u:User) " +
                       "WHERE u.department = $dept " +
                       "RETURN u.status AS status, count(u) AS total " +
                       "ORDER BY total DESC";

        Collection<Map<String, Object>> rows = neo4jClient.query(query)
                .bind(department).to("dept")
                .fetch().all();

        Map<String, Object> result = new LinkedHashMap<>();
        long grandTotal = 0;

        for (Map<String, Object> row : rows) {
            String status = row.get("status") != null ? row.get("status").toString() : "UNKNOWN";
            long count = ((Number) row.get("total")).longValue();
            result.put(status.toLowerCase() + "Count", count);
            grandTotal += count;
        }

        result.put("department", department);
        result.put("totalUsers", grandTotal);
        result.put("timestamp", new Date());
        return ResponseEntity.ok(result);
    }
}
