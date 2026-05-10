package com.circleguard.e2e;

import com.circleguard.e2e.config.ServiceUrls;
import com.circleguard.e2e.support.AuthHelper;
import com.circleguard.e2e.support.AuthHelper.LoginResult;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Flow 5 — Dashboard Analytics Chain with K-Anonymity Validation
 *
 * Black-box: POST /api/v1/auth/login              (auth-service)
 *            GET  /api/v1/analytics/health-board  (dashboard-service → promotion-service)
 *            GET  /api/v1/analytics/summary       (dashboard-service → promotion-service)
 *            GET  /api/v1/analytics/time-series   (dashboard-service)
 *
 * Validates that:
 *   a) The analytics endpoints are reachable end-to-end (dashboard → promotion chain works).
 *   b) K-anonymity is enforced: no numeric count field in any response holds a value
 *      in the range [1, 4] — those must be replaced with "<5" by KAnonymityFilter.
 *   c) The time-series endpoint respects the 'period' query parameter.
 */
@Tag("e2e")
@DisplayName("E2E Flow 5 – Dashboard analytics chain with k-anonymity (auth → dashboard → promotion)")
class DashboardAnalyticsChainE2E {

    @Test
    @Disabled("Disabled: requires additional service data or permissions not available in CI environment")
    @DisplayName("Health-board endpoint returns 200 with valid structure")
    void healthBoard_IsReachableAndReturnsValidStructure() {
        LoginResult admin = AuthHelper.loginAsAdmin();

        Response response = given()
                .baseUri(ServiceUrls.DASHBOARD)
                .spec(admin.authenticated())
                .when()
                .get("/api/v1/analytics/health-board")
                .then()
                .statusCode(200)
                .extract().response();

        // Dashboard must return a JSON object (not null, not empty array)
        assertThat(response.getBody().asString()).isNotBlank();
    }

    @Test
    @Disabled("Disabled: requires additional service data or permissions not available in CI environment")
    @DisplayName("Campus summary endpoint returns 200 — dashboard-to-promotion chain is alive")
    void summary_PromotionServiceChainIsAlive() {
        LoginResult admin = AuthHelper.loginAsAdmin();

        given()
                .baseUri(ServiceUrls.DASHBOARD)
                .spec(admin.authenticated())
                .when()
                .get("/api/v1/analytics/summary")
                .then()
                .statusCode(200);
    }

    @Test
    @Disabled("Disabled: requires additional service data or permissions not available in CI environment")
    @DisplayName("No *Count field in health-board response contains a raw integer in [1, 4]")
    void healthBoard_KAnonymityFilterIsApplied() {
        LoginResult admin = AuthHelper.loginAsAdmin();

        JsonPath json = given()
                .baseUri(ServiceUrls.DASHBOARD)
                .spec(admin.authenticated())
                .when()
                .get("/api/v1/analytics/health-board")
                .then()
                .statusCode(200)
                .extract().jsonPath();

        // Traverse all fields; any field ending with "Count" must not be a raw int in [1,4]
        Map<String, Object> body = json.getMap("$");
        if (body != null) {
            body.forEach((key, value) -> {
                if (key.endsWith("Count") && value instanceof Number) {
                    long count = ((Number) value).longValue();
                    assertThat(count)
                            .as("Field '%s' = %d violates k-anonymity (must be 0 or >= 5)", key, count)
                            .satisfiesAnyOf(
                                    c -> assertThat(c).isZero(),
                                    c -> assertThat(c).isGreaterThanOrEqualTo(5L)
                            );
                }
                if (key.endsWith("Count") && value instanceof String) {
                    // Masked value — must be "<K" format
                    assertThat((String) value)
                            .as("Masked field '%s' must start with '<'", key)
                            .startsWith("<");
                }
            });
        }
    }

    @Test
    @Disabled("Disabled: requires additional service data or permissions not available in CI environment")
    @DisplayName("Time-series endpoint respects 'period' query parameter")
    void timeSeries_RespondsToHourlyAndDailyPeriods() {
        LoginResult admin = AuthHelper.loginAsAdmin();

        for (String period : new String[]{"hourly", "daily"}) {
            given()
                    .baseUri(ServiceUrls.DASHBOARD)
                    .spec(admin.authenticated())
                    .queryParam("period", period)
                    .queryParam("limit", 24)
                    .when()
                    .get("/api/v1/analytics/time-series")
                    .then()
                    .statusCode(200);
        }
    }
}
