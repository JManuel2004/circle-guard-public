package com.circleguard.dashboard.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KAnonymityFilterTest {

    private final KAnonymityFilter filter = new KAnonymityFilter();

    // Rule A: when totalUsers < K the entire result is masked
    @Test
    void apply_WhenTotalUsersBelowK_MasksEntireResult() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("department", "Engineering");
        stats.put("timestamp", 1_700_000_000L);
        stats.put("totalUsers", 3);
        stats.put("activeCount", 2);
        stats.put("suspectCount", 1);

        Map<String, Object> result = filter.apply(stats);

        assertThat(result.get("note")).isEqualTo("Insufficient data for privacy");
        assertThat(result.get("totalUsers")).isEqualTo("<5");
        assertThat(result.get("department")).isEqualTo("Engineering");
        assertThat(result.get("timestamp")).isEqualTo(1_700_000_000L);
        // individual count fields must NOT leak
        assertThat(result).doesNotContainKey("activeCount");
        assertThat(result).doesNotContainKey("suspectCount");
    }

    // Rule B: only count fields < K are masked; count == 0 is left untouched
    @Test
    void apply_WhenOnlySomeCountFieldsBelowK_MasksThoseFieldsOnly() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", 100);
        stats.put("activeCount", 94);
        stats.put("suspectCount", 3);   // below K=5 → must be masked
        stats.put("confirmedCount", 0); // 0 → NOT masked (condition: count > 0)
        stats.put("recoveredCount", 5); // exactly K → NOT masked

        Map<String, Object> result = filter.apply(stats);

        assertThat(result.get("activeCount")).isEqualTo(94);
        assertThat(result.get("suspectCount")).isEqualTo("<5");
        assertThat(result.get("confirmedCount")).isEqualTo(0);
        assertThat(result.get("recoveredCount")).isEqualTo(5);
    }

    @Test
    void apply_WhenStatsNull_ReturnsEmptyMap() {
        assertThat(filter.apply(null)).isEmpty();
    }
}
