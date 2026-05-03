package com.circleguard.dashboard.service;

import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Privacy-First K-Anonymity Engine (Story 7.5, FR-23).
 * Masks any metric group with fewer than K users to prevent
 * individual identification in small departments/buildings.
 */
@Component
public class KAnonymityFilter {

    private static final int DEFAULT_K = 5;

    /**
     * Applies k-anonymity masking to a stats map.
     * Any count field ending with "Count" that is below K is replaced with "<K".
     * The totalUsers field is also masked if below K.
     */
    public Map<String, Object> apply(Map<String, Object> stats) {
        return apply(stats, DEFAULT_K);
    }

    public Map<String, Object> apply(Map<String, Object> stats, int k) {
        if (stats == null) return Map.of();

        Map<String, Object> filtered = new LinkedHashMap<>(stats);
        long total = 0;

        // Check if total population is below K
        if (filtered.containsKey("totalUsers")) {
            Object val = filtered.get("totalUsers");
            if (val instanceof Number) {
                total = ((Number) val).longValue();
            }
        }

        if (total > 0 && total < k) {
            // Mask entire result for populations smaller than K
            Map<String, Object> masked = new LinkedHashMap<>();
            masked.put("note", "Insufficient data for privacy");
            if (filtered.containsKey("department")) {
                masked.put("department", filtered.get("department"));
            }
            if (filtered.containsKey("timestamp")) {
                masked.put("timestamp", filtered.get("timestamp"));
            }
            masked.put("totalUsers", "<" + k);
            return masked;
        }

        // Mask individual count fields below K
        for (Map.Entry<String, Object> entry : new LinkedHashMap<>(filtered).entrySet()) {
            if (entry.getKey().endsWith("Count") && entry.getValue() instanceof Number) {
                long count = ((Number) entry.getValue()).longValue();
                if (count > 0 && count < k) {
                    filtered.put(entry.getKey(), "<" + k);
                }
            }
        }

        return filtered;
    }
}
